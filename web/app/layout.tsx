import type { Metadata } from "next"
import "./globals.css"
import { Footer } from "./components/Footer"

export const metadata: Metadata = {
  title: "BaselineMS gait pipeline",
  description:
    "On-device IMU gait analysis pipeline (Madgwick orientation + ZUPT stride integration) validated against four public motion-capture-anchored gait datasets.",
}

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className="min-h-screen flex flex-col">
        <main className="flex-1">{children}</main>
        <Footer />
      </body>
    </html>
  )
}
