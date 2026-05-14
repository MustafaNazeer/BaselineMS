import type { NextConfig } from "next"
import createMDX from "@next/mdx"

const withMDX = createMDX({
  extension: /\.mdx?$/,
})

const nextConfig: NextConfig = {
  output: "export",
  pageExtensions: ["ts", "tsx", "md", "mdx"],
  images: { unoptimized: true },
  trailingSlash: true,
}

export default withMDX(nextConfig)
