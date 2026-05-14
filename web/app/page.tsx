import { Hero } from "./components/Hero"
import { ValidationTable } from "./components/ValidationTable"
import { loadValidationSummary } from "@/lib/data"

export default async function Page() {
  const summary = await loadValidationSummary()
  return (
    <>
      <Hero />
      <ValidationTable datasets={summary.datasets} />
    </>
  )
}
