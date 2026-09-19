import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  // A self-contained server bundle, so the image carries no node_modules tree.
  output: "standalone",
};

export default withNextIntl(nextConfig);
