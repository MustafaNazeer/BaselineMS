import Link from "next/link"

export function Hero() {
  return (
    <section className="border-b border-[var(--color-muted)] py-20 px-6">
      <div className="max-w-5xl mx-auto">
        <div className="text-xs uppercase tracking-widest text-[var(--color-ink-soft)] mb-3">
          BaselineMS gait analysis
        </div>
        <h1 className="text-4xl sm:text-5xl font-semibold mb-5 leading-tight">
          A smartphone IMU gait pipeline, validated.
        </h1>
        <p className="text-lg text-[var(--color-ink-soft)] max-w-3xl mb-7 leading-relaxed">
          BaselineMS is an Android app for people with Multiple Sclerosis. Its
          gait module turns 30 seconds of phone-pocket IMU data into cadence,
          stride length, and step time variability via a from-scratch Madgwick
          orientation filter and a Zero-Velocity-Update stride integrator. This
          page reports the pipeline&rsquo;s accuracy across four public motion-capture-anchored
          gait datasets covering roughly 110 unique participants.
        </p>
        <div className="flex gap-6 text-sm">
          <a href="https://github.com/MustafaNazeer/BaselineMS">View source on GitHub</a>
          <Link href="/methodology">Methodology</Link>
          <a href="#validation">Jump to results</a>
        </div>
      </div>
    </section>
  )
}
