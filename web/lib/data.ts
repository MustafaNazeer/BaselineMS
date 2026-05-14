import fs from "node:fs/promises"
import path from "node:path"
import type { ValidationSummary } from "./types"

const PUBLIC_DIR = path.join(process.cwd(), "public")

export async function loadValidationSummary(): Promise<ValidationSummary> {
  const raw = await fs.readFile(path.join(PUBLIC_DIR, "validation-summary.json"), "utf8")
  return JSON.parse(raw)
}
