# Builder: Elixir + Node in one stage - unlike a plain Tailwind CLI setup,
# the Vite build resolves the Phoenix JS clients from deps/ and the colocated
# hooks from _build/, so assets can only build after mix compile.
FROM elixir:1.20-otp-29-alpine AS builder

RUN apk add --no-cache build-base git nodejs npm

WORKDIR /app

ENV MIX_ENV=prod

RUN mix local.hex --force && mix local.rebar --force

COPY mix.exs mix.lock ./
RUN mix deps.get --only prod \
  && mkdir config
COPY config/config.exs config/prod.exs config/runtime.exs config/
RUN mix deps.compile

COPY package.json package-lock.json vite.config.js ./
RUN npm ci

COPY lib lib
COPY priv priv
COPY assets assets

RUN mix compile \
  && npm run build \
  && mix phx.digest \
  && mix release

FROM alpine:3.23

RUN apk add --no-cache libstdc++ openssl ncurses-libs ca-certificates

# Litestream streams the SQLite WAL to object storage (continuous backup).
COPY --from=litestream/litestream:0.5 /usr/local/bin/litestream /usr/local/bin/litestream

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

ENV MIX_ENV=prod
ENV PHX_SERVER=true
ENV DATABASE_PATH=/data/charset.db

RUN addgroup -S app && adduser -S -G app app \
  && mkdir /data && chown app:app /data

COPY --from=builder --chown=app:app /app/_build/prod/rel/app ./
COPY --chown=app:app docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

USER app

VOLUME /data
EXPOSE 4000

ENTRYPOINT ["/app/docker-entrypoint.sh"]
