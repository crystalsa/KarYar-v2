# چک‌لیست اعتبارسنجی و انتشار کاریار (Release Checklist)

این سند مراحل گام‌به‌گام بررسی، تست و انتشار نسخه‌های جدید اپلیکیشن «کاریار» را ارائه می‌دهد.

---

## ۱. چک‌لیست اعتبارسنجی فنی (قبل از مرج و ریلیز)

- [ ] **اجرای تست‌های واحد و مهاجرت دیتابیس:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
  *(هر ۵۲ تست بدون خطا پاس شوند).*

- [ ] **تست سلامت کامپایل و بیلد Debug:**
  ```bash
  ./gradlew assembleDebug
  ```

- [ ] **تست بیلد Release محلی (با کلید تستی یا محیطی):**
  ```bash
  KEYSTORE_PATH=/path/to/keystore.jks STORE_PASSWORD=*** KEY_ALIAS=*** KEY_PASSWORD=*** ./gradlew assembleRelease
  ```

- [ ] **بررسی عدم نشت اسکیما در فایل APK:**
  ```bash
  unzip -l app/build/outputs/apk/release/app-release.apk | grep -i "json"
  ```
  *(خروجی باید خالی باشد و هیچ فایل اسکیمایی در APK وجود نداشته باشد).*

- [ ] **بررسی اندازه متون و آیکون‌ها:**
  - تطابق نسخه و برچسب‌های رشته‌ای با فونت وزیرمتن و زبان فارسی.

---

## ۲. چک‌لیست انتشار نسخه جدید (Release Pipeline)

1. **به‌روزرسانی شماره نسخه:**
   - در فایل `app/build.gradle.kts`:
     - افزایش `versionCode` (مثلاً از `1` به `2`).
     - تنظیم `versionName` (مثلاً `"1.0.1"`).
   - کامیت و پوش به شاخه `main`.

2. **تنظیم Secretهای مخزن گیت‌هاب (فقط یک‌بار):**
   - به مسیر **Settings > Secrets and variables > Actions** بروید.
   - ۴ سکرت زیر را ثبت کنید:
     - `RELEASE_KEYSTORE_BASE64`: خروجی دستور `base64 -w 0 upload-keystore.jks`
     - `RELEASE_KEYSTORE_PASSWORD`: رمز فایل KeyStore
     - `RELEASE_KEY_ALIAS`: نام Alias کلید
     - `RELEASE_KEY_PASSWORD`: رمز اختصاصی کلید

3. **ایجاد و ارسال تگ نسخه:**
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. **تأیید خودکار اکشن Release در گیت‌هاب:**
   - پایپ‌لاین `release.yml` اجرا می‌شود.
   - تست‌ها پاس می‌شوند.
   - APK ریلیز امضاشده تولید شده و به صفحه Releases به همراه گزارش خودکار الصاق می‌گردد.
