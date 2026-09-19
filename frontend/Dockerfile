FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# `output: "standalone"` leaves a server bundle with only the modules it uses.
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN addgroup -S aether && adduser -S -G aether aether
COPY --from=build /app/public ./public
COPY --from=build --chown=aether:aether /app/.next/standalone ./
COPY --from=build --chown=aether:aether /app/.next/static ./.next/static
USER aether
EXPOSE 3000
CMD ["node", "server.js"]
