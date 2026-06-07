export function Footer() {
  return (
    <footer className="border-t border-[var(--color-muted)] py-8 px-6 text-xs text-[var(--color-ink-soft)]">
      <div className="max-w-5xl mx-auto flex flex-col sm:flex-row sm:justify-between gap-2">
        <div>BaselineMS &mdash; on-device gait analysis for people with MS.</div>
        <div className="flex gap-5">
          <a href="https://github.com/MustafaNazeer/BaselineMS">GitHub</a>
          <a href="/methodology">Methodology</a>
          <a href="/privacy-policy">Privacy policy</a>
        </div>
      </div>
    </footer>
  )
}
