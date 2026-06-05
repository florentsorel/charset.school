import path from "node:path"
import { defineConfig } from "vite"
import tailwindcss from "@tailwindcss/vite"

// Phoenix compiles colocated LiveView hooks into the Mix build directory.
const mixEnv = process.env.MIX_ENV || "dev"
const root = import.meta.dirname

export default defineConfig(({ mode }) => ({
  root: path.resolve(root, "assets"),
  plugins: [tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(root, "assets/js"),
      // Resolve the Phoenix JS clients from the Mix deps, so their versions
      // always match mix.lock (no duplicate npm copies to keep in sync).
      phoenix_html: path.resolve(root, "deps/phoenix_html/priv/static/phoenix_html.js"),
      phoenix: path.resolve(root, "deps/phoenix/priv/static/phoenix.mjs"),
      phoenix_live_view: path.resolve(root, "deps/phoenix_live_view/priv/static/phoenix_live_view.esm.js"),
      "phoenix-colocated": path.resolve(root, "_build", mixEnv, "phoenix-colocated"),
    },
  },
  build: {
    target: "es2022",
    outDir: path.resolve(root, "priv/static/assets"),
    emptyOutDir: true,
    sourcemap: mode === "development",
    minify: mode !== "development",
    rollupOptions: {
      // Single entry: app.js imports app.css, emitted as a separate stylesheet
      // (root.html.heex links /assets/css/app.css and /assets/js/app.js).
      input: { app: path.resolve(root, "assets/js/app.js") },
      output: {
        entryFileNames: "js/[name].js",
        chunkFileNames: "js/[name]-[hash].js",
        assetFileNames: (assetInfo) =>
          assetInfo.names?.some((name) => name.endsWith(".css"))
            ? "css/[name][extname]"
            : "[name][extname]",
      },
      // Fonts and images are served from priv/static, not bundled.
      external: [/^\/fonts\//, /^\/images\//],
    },
  },
}))
