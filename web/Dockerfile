FROM node:24-alpine AS builder

RUN apk add --no-cache python3 make g++

WORKDIR /app

RUN corepack enable

COPY . .

RUN pnpm install --frozen-lockfile
RUN NODE_OPTIONS=--max-old-space-size=6144 pnpm build

FROM node:24-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000

USER node

COPY --from=builder --chown=node:node /app/.output ./

EXPOSE 3000
CMD ["node", "server/index.mjs"]
