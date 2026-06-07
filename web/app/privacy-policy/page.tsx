import type { Metadata } from "next"

export const metadata: Metadata = {
  title: "Privacy policy - BaselineMS",
}

export default function PrivacyPolicyPage() {
  return (
    <article className="prose-doc px-6 py-16">
      <h1>BaselineMS privacy policy</h1>

      <p>
        <strong>Effective date:</strong> 2026-06-06
        <br />
        <strong>Application name:</strong> BaselineMS
        <br />
        <strong>Application developer:</strong> Mustafa Nazeer (individual
        developer, Grand Prairie, Texas, United States)
        <br />
        <strong>Contact email:</strong> Mustafa.nazeer06@gmail.com
        <br />
        <strong>Application package identifier:</strong>{" "}
        <code>com.mustafanazeer.baselinems</code>
        <br />
        <strong>Distribution channel:</strong> Google Play Console internal
        testing track (v1 beta)
        <br />
        <strong>Geography:</strong> United States only (the v1 beta is
        restricted to the United States; the Play Console internal track sets
        country and region targeting to United States only)
      </p>

      <p>
        This document is the privacy policy for the BaselineMS Android
        application. It describes what data the application processes, where
        the data lives, who can access the data, and what your rights are as a
        user. The policy is written in plain language because the application
        is intended for personal use by people living with multiple sclerosis
        and their caregivers, not for a corporate compliance audience.
      </p>

      <h2>1. Summary in one paragraph</h2>

      <p>
        BaselineMS is a personal wellness application that lets you self
        administer a five test neurological battery once a week, track your
        results longitudinally on your own device, and produce a personal
        record you can share with your neurologist. The application processes
        every piece of data on your device. The application does not transmit
        any data to any server. The application has no backend, no analytics,
        no advertising, and no third party sharing. The application does not
        declare the <code>android.permission.INTERNET</code> permission, which
        means it cannot send data over the network even if a bug somewhere
        asked it to. You can verify this on the Google Play listing by
        inspecting the declared permissions. Uninstalling the application
        removes every byte of stored data from your device.
      </p>

      <h2>2. What the application does</h2>

      <p>The application administers five short tests once a week:</p>

      <ol>
        <li>A bilateral tap test on the touchscreen.</li>
        <li>
          A 30 second walking test using the phone&rsquo;s motion sensors.
        </li>
        <li>
          A low contrast vision test using the touchscreen for input and the
          front camera for an ambient lighting and viewing distance gate at
          the start of the test.
        </li>
        <li>
          A symbol substitution test on the touchscreen using a numeric
          keypad.
        </li>
        <li>A 30 second voice reading test using the microphone.</li>
      </ol>

      <p>
        The application computes objective, quantitative features from each
        test (for example, taps per second, walking cadence, vision letters
        correct, symbol substitutions per 90 seconds, voice quality
        measurements). It stores those features on your device. It produces a
        PDF report on demand summarizing your trends across sessions, which
        you can share with your neurologist through the standard Android Share
        Intent.
      </p>

      <p>
        The application is not a medical device. It does not diagnose or treat
        any condition. Results are not validated for any clinical use. Do not
        change your treatment based on these results. Share results with your
        neurologist for clinical decisions.
      </p>

      <h2>3. What data the application processes</h2>

      <p>
        The application processes the following categories of data, all on
        your device:
      </p>

      <h3>3.1 Sensor inputs during each test</h3>

      <ul>
        <li>
          <strong>Touchscreen events</strong> during the tap test, vision
          test, and symbol substitution test. Coordinates, timestamps, and the
          identifier of the on screen target tapped.
        </li>
        <li>
          <strong>Inertial measurement unit traces</strong> during the gait
          test. Three axis linear acceleration, three axis gyroscope, and the
          rotation vector quaternion at approximately 100 hertz for the 30
          second walking window.
        </li>
        <li>
          <strong>Camera frames from the front camera</strong> during the
          vision test&rsquo;s lighting and viewing distance gate at the start
          of the test. The application reads the ambient luminance and the
          apparent face size for the gate, then releases the camera before the
          reading task begins. The camera frames themselves are not retained;
          only the derived lux estimate and the derived face distance estimate
          are kept in memory long enough for the gate decision and are
          discarded immediately after.
        </li>
        <li>
          <strong>Microphone audio</strong> during the voice test. 44.1
          kilohertz mono PCM during the 30 second reading window. The audio is
          processed in memory to extract six acoustic features (jitter,
          shimmer, harmonics to noise ratio, fundamental frequency standard
          deviation, speaking rate, pause fraction). The raw audio is
          discarded after feature extraction by default. If you opt in to
          retain the raw audio under Settings, the WAV file is stored in the
          application&rsquo;s private internal storage; you can turn the
          toggle off at any time and the saved recordings are deleted from
          your device immediately. Saved recordings are kept only on this
          phone and are never used to identify you.
        </li>
      </ul>

      <h3>3.2 Derived feature values per test result</h3>

      <p>
        After each test runs, the application stores a small set of scalar
        feature values in an encrypted on device database. The feature set is
        documented in the application&rsquo;s About screen. No raw sensor data
        is persisted by default; the only exception is the IMU trace for the
        gait test (compressed to roughly 60 kilobytes per trial; retained so
        that the gait pipeline can be replayed against the trace if needed)
        and the optional opt in raw voice audio described above.
      </p>

      <h3>3.3 A small user profile</h3>

      <p>
        A one time setup screen at first launch asks for your year of birth
        (not your full date of birth), your biological sex (with an
        &ldquo;undisclosed&rdquo; option), your dominant hand, your height in
        centimeters for stride length calibration, and optionally your MS type
        if you choose to share that. Every field is optional except the
        height, which is needed for the gait calibration. The profile is
        stored on your device alongside the test results. The application does
        not collect your name, your email, your phone number, your full date
        of birth, your address, your IP address, or any other personal
        identifier.
      </p>

      <h3>3.4 What the application does not collect</h3>

      <p>
        The application does not collect or process the following categories
        of data:
      </p>

      <ul>
        <li>
          Your name, your email, your phone number, your home or work address.
        </li>
        <li>Your contacts, your call log, your text messages.</li>
        <li>
          Your location (the application does not declare any location
          permission).
        </li>
        <li>
          Your full date of birth (only the year of birth, for an age based
          reference scale, and only at your discretion).
        </li>
        <li>
          Your government identifiers (Social Security number, driver&rsquo;s
          license, passport).
        </li>
        <li>
          Your financial information (the application is free and does not
          process any payment).
        </li>
        <li>
          Your account or login information for any other service (the
          application has no login of any kind).
        </li>
        <li>
          Cookies, advertising identifiers, or third party analytics
          identifiers.
        </li>
      </ul>

      <h2>4. Where the data lives</h2>

      <p>
        All data the application processes lives on your device in the
        application&rsquo;s private internal storage at{" "}
        <code>/data/data/com.mustafanazeer.baselinems/</code>, which is
        protected by Android File Based Encryption. On Android 12 and later,
        this directory is encrypted at rest with keys tied to your device
        unlock credentials and is inaccessible to other applications. The
        application&rsquo;s Room database file lives at{" "}
        <code>/data/data/com.mustafanazeer.baselinems/databases/</code>. The
        optional retained voice audio files live under{" "}
        <code>/data/data/com.mustafanazeer.baselinems/files/voice-audio/</code>
        . Exported PDF and CSV files live temporarily under{" "}
        <code>/data/data/com.mustafanazeer.baselinems/cache/exports/</code>{" "}
        before you send them through the Android Share Intent; the cache
        directory is cleared by the operating system periodically and the
        export files are not retained after the share action completes.
      </p>

      <h2>5. Where the data does not go</h2>

      <p>
        BaselineMS does not sell or share your personal information. There is
        no third party recipient of your data. The application has no backend,
        no analytics, and no advertising. All data are processed on your
        device.
      </p>

      <p>
        The application does not declare the{" "}
        <code>android.permission.INTERNET</code> permission. The application
        cannot connect to any server, send any request, post any analytics
        event, or transmit any data over a network. You can verify this on the
        Google Play listing by viewing the declared permissions; the list will
        show CAMERA and RECORD_AUDIO, and no INTERNET permission.
      </p>

      <p>
        The only outward facing action the application performs is the Android
        Share Intent, which you initiate from the Reports screen when you want
        to send a PDF or CSV report somewhere. The Share Intent hands the file
        to a destination you pick (your own email client, a cloud drive you
        control, a messaging app, the Android Files app). The application has
        no part in transmitting the file once the Share Intent fires; the
        destination application you picked is responsible for what happens
        next. The application does not pre populate any recipient address or
        default share target; you make every transmission decision yourself.
      </p>

      <h2>6. Permissions the application declares</h2>

      <p>
        The application declares exactly two Android runtime permissions:
      </p>

      <ul>
        <li>
          <strong>
            <code>android.permission.CAMERA</code>
          </strong>
          , used only by the vision test&rsquo;s lighting and viewing distance
          gate at the start of the test. The application requests this
          permission the first time you start a vision test. If you deny the
          permission, the vision test is unavailable on this device; you can
          still run the other four tests. The application does not use the
          camera for any purpose other than the vision test gate.
        </li>
        <li>
          <strong>
            <code>android.permission.RECORD_AUDIO</code>
          </strong>
          , used only by the voice test for the 30 second reading window. The
          application requests this permission the first time you start a
          voice test. If you deny the permission, the voice test is
          unavailable on this device; you can still run the other four tests.
          The application does not record audio outside the active reading
          window.
        </li>
      </ul>

      <p>
        The application does not declare any of the following permissions:
      </p>

      <ul>
        <li>
          <code>android.permission.INTERNET</code>,{" "}
          <code>android.permission.ACCESS_NETWORK_STATE</code>,{" "}
          <code>android.permission.ACCESS_WIFI_STATE</code>, or any other
          network permission. The application cannot reach a server.
        </li>
        <li>
          <code>android.permission.ACCESS_FINE_LOCATION</code>,{" "}
          <code>android.permission.ACCESS_COARSE_LOCATION</code>, or any other
          location permission. The application does not know where you are.
        </li>
        <li>
          <code>android.permission.READ_CONTACTS</code>,{" "}
          <code>android.permission.READ_CALL_LOG</code>,{" "}
          <code>android.permission.READ_SMS</code>, or any other personal data
          permission.
        </li>
        <li>
          <code>android.permission.READ_EXTERNAL_STORAGE</code>,{" "}
          <code>android.permission.WRITE_EXTERNAL_STORAGE</code>, or any
          shared storage permission. The application reads and writes only to
          its own private internal storage.
        </li>
      </ul>

      <p>
        The two declared permissions and the absence of every other permission
        can be verified by inspecting the application&rsquo;s manifest on the
        Google Play listing or by running <code>aapt dump permissions</code>{" "}
        against the installed APK on a developer device.
      </p>

      <h2>7. The voice test in particular</h2>

      <p>
        The voice test captures audio while you read a passage. Acoustic
        features are extracted and stored on your device; the raw audio is
        discarded by default. You can opt in under Settings to retain the raw
        audio for personal review; the raw audio is stored only on your device
        and is deleted immediately when you turn the setting off.
      </p>

      <p>
        Saved recordings are kept only on this phone and are never used to
        identify you. The application does not produce a voiceprint, does not
        perform speaker recognition, and does not use the raw audio for any
        purpose other than your own personal playback if you choose to retain
        it. The application does not transmit the raw audio anywhere; the same{" "}
        <code>android.permission.INTERNET</code> posture from Section 5
        applies to the voice test as it does to every other test.
      </p>

      <h2>8. Your rights and how to exercise them</h2>

      <p>
        Because every piece of data the application processes lives on your
        device, you have direct physical and software access to every piece of
        data the application has about you. You do not need to file a data
        subject access request with anyone. The rights below describe how you
        exercise the standard data subject rights against your own device.
      </p>

      <ul>
        <li>
          <strong>Right of access.</strong> Use the application&rsquo;s Export
          feature on the Reports screen to produce a PDF report and a CSV file
          containing your data on your own device.
        </li>
        <li>
          <strong>Right to deletion.</strong> Uninstall the application to
          delete every piece of data the application has stored.
          Android&rsquo;s package manager removes the application&rsquo;s
          private internal storage on uninstall, including the Room database,
          the IMU traces, the optional retained voice audio, and any cached
          export files. If you want to delete only the retained voice audio
          while keeping your test history, toggle the Settings voice retention
          switch off; the saved recordings are deleted from your device
          immediately.
        </li>
        <li>
          <strong>Right to rectification.</strong> Re run any test to produce
          a new result. The application&rsquo;s data model is append only at
          the session level (a completed session is immutable), so the new
          result is added alongside the older result; the older result is
          preserved for your historical view. If you want to discard a
          specific older session, the application&rsquo;s data deletion path
          via uninstall is the operative path; per session deletion within the
          application is not currently supported and is tracked as a future
          feature.
        </li>
        <li>
          <strong>Right to object to processing.</strong> Uninstall the
          application. There is no consent toggle for processing because every
          processing event is triggered by your direct action (you choose to
          start a session; you choose to start the voice test; you choose to
          export a report). If you do not want any processing to happen, you
          do not start any session.
        </li>
        <li>
          <strong>Right to data portability.</strong> The Export feature
          produces a long format CSV with one row per feature value per test
          result. The CSV is portable to any spreadsheet application, R,
          Python, or other data analysis tool you use.
        </li>
        <li>
          <strong>Right to lodge a complaint.</strong> Because the v1 beta is
          restricted to the United States, the standard United States consumer
          protection authorities apply: the United States Federal Trade
          Commission, the California Attorney General if you reside in
          California, the Illinois Attorney General if you reside in Illinois,
          the Washington State Attorney General if you reside in Washington,
          the Texas Attorney General if you reside in Texas. The applicable
          state authorities and the federal authorities are the appropriate
          venues for a complaint about this application&rsquo;s data
          practices.
        </li>
      </ul>

      <h2>9. Children</h2>

      <p>
        The application is targeted at adults 18 and over. The application is
        not designed for or appropriate for use by minors. The Play Console
        target audience setting is configured for 18 and over only. The
        application does not collect data from children under 13 (the federal
        Children&rsquo;s Online Privacy Protection Act threshold) and does not
        collect data from minors more generally. If a minor uses the
        application in spite of the age targeting, the minor&rsquo;s parent or
        guardian can delete all stored data by uninstalling the application.
      </p>

      <h2>10. Changes to this policy</h2>

      <p>
        This privacy policy is versioned in the BaselineMS source repository
        at <code>web/app/privacy-policy/page.tsx</code>. If the policy changes
        (for example, if a future v1.1 release adds the European Union to the
        beta cohort and the policy adds a General Data Protection Regulation
        section), the change is committed to the repository with a dated
        commit message and the effective date at the top of this document is
        updated. The hosted URL serves the current version. Material changes
        are surfaced in the application&rsquo;s About screen so that current
        users see the change at the next launch after the change is published.
      </p>

      <h2>11. Contact</h2>

      <p>
        Questions about this privacy policy, about the application&rsquo;s
        data handling, or about a specific request related to your data on
        your device should be sent by email to Mustafa.nazeer06@gmail.com. The
        contact email is reviewed by Mustafa Nazeer (the individual
        developer). There is no support team; the v1 beta is operated by a
        single developer.
      </p>

      <h2>12. Regulatory posture statements</h2>

      <p>
        This section is included for completeness and for any reviewer (Google
        Play, a state regulator, a journalist, a clinician) who wants the
        regulatory framing in plain English.
      </p>

      <ul>
        <li>
          <strong>Food and Drug Administration posture.</strong> The
          application falls under the United States Food and Drug
          Administration&rsquo;s 2026-01-06 reissue of the General Wellness:
          Policy for Low Risk Devices guidance. The application is noninvasive
          and low risk (a smartphone application with sensor inputs from the
          touchscreen, the inertial measurement unit, the front camera, and
          the microphone; none of these are invasive, none introduce safety
          risk, none use lasers or radiation). The application carries only
          wellness focused claims; it does not diagnose, treat, cure, or
          prevent any disease. The application is not a medical device and is
          not regulated as Software as a Medical Device.
        </li>
        <li>
          <strong>
            Health Insurance Portability and Accountability Act posture.
          </strong>{" "}
          The application is not a covered entity, not a business associate,
          and not a healthcare clearinghouse. The application does not create,
          receive, maintain, or transmit protected health information for or
          on behalf of any covered entity. The application is outside the
          scope of the HIPAA Privacy Rule and the HIPAA Security Rule.
        </li>
        <li>
          <strong>California Consumer Privacy Act posture.</strong> The
          application is not a &ldquo;business&rdquo; under the California
          Consumer Privacy Act&rsquo;s three thresholds (annual gross revenue
          over USD 25 million; annual buying, selling, or sharing of personal
          information of 100,000 or more consumers; annual revenue of 50
          percent or more derived from selling or sharing personal
          information). Even read conservatively as if the application were a
          &ldquo;business,&rdquo; the application does not sell or share
          personal information per California Civil Code Section 1798.140(d)
          and Section 1798.140(ah). The application has no third party
          recipient of data.
        </li>
        <li>
          <strong>Illinois Biometric Information Privacy Act posture.</strong>{" "}
          The application&rsquo;s voice test captures audio while you read a
          passage. The data subject (you) is the collector; the application is
          not a &ldquo;private entity&rdquo; in the BIPA Section 10 sense
          collecting biometric data from another data subject. The acoustic
          features extracted (jitter, shimmer, harmonics to noise ratio,
          fundamental frequency standard deviation, speaking rate, pause
          fraction) are scalar voice quality measurements; they are not a
          &ldquo;voiceprint&rdquo; in the BIPA Section 10 sense and are not
          used to identify you. The optional retained raw audio is stored only
          on your device and is never used to identify you.
        </li>
        <li>
          <strong>Washington My Health My Data Act posture.</strong> The
          application does not sell or share consumer health data per the
          Act&rsquo;s definitions. The on device only architecture and the no
          transmission posture keep the application outside the Act&rsquo;s
          controller and processor obligations.
        </li>
        <li>
          <strong>
            Google Play Health Content and Services policy posture.
          </strong>{" "}
          The application is positioned in the wellness frame, not the medical
          device frame. The application&rsquo;s user facing copy carries the
          wellness disclaimer verbatim in the in app About screen, on the PDF
          report cover, and on every per page footer of the PDF report. The
          disclaimer says: &ldquo;This is not a medical device. It does not
          diagnose or treat any condition. Do not change your treatment based
          on these results. Share with your neurologist for clinical
          decisions.&rdquo; Source: Google Play Developer Policy Center,
          Health content and services,{" "}
          <code>
            https://support.google.com/googleplay/android-developer/answer/9888379
          </code>
          .
        </li>
      </ul>

      <h2>13. References for the curious reader</h2>

      <ul>
        <li>
          BaselineMS source repository:{" "}
          <code>https://github.com/MustafaNazeer/BaselineMS</code>. The
          application&rsquo;s source code is open and inspectable; the
          manifest at <code>app/src/main/AndroidManifest.xml</code> shows the
          two declared permissions and the absence of the INTERNET permission.
        </li>
        <li>
          BaselineMS validation showcase:{" "}
          <code>https://web-pi-one-62.vercel.app</code>. The page documents
          the gait pipeline&rsquo;s validation methodology against three
          public datasets.
        </li>
        <li>
          Google Play Developer Policy Center, Health content and services:{" "}
          <code>
            https://support.google.com/googleplay/android-developer/answer/9888379
          </code>
          .
        </li>
        <li>
          Google Play Developer Policy Center, Data safety section overview:{" "}
          <code>
            https://support.google.com/googleplay/android-developer/answer/10787469
          </code>
          .
        </li>
        <li>
          United States Food and Drug Administration, General Wellness: Policy
          for Low Risk Devices (2026-01-06 reissue):{" "}
          <code>
            https://www.fda.gov/regulatory-information/search-fda-guidance-documents/general-wellness-policy-low-risk-devices
          </code>
          .
        </li>
      </ul>

      <p>End of privacy policy.</p>
    </article>
  )
}
