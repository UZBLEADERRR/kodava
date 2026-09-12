# Kodava Studio

G'oyadan video storyboard yasaydigan Android ilova. Siz g'oyani yozasiz — ilova senariy yozadi,
uni sahnalarga bo'ladi, har bir sahnaga rasm chizadi va **aktyorlarning yuzini o'zgartirmaydi**.
Tayyor kadrlarni galereyaga yuklab olasiz yoki ulardan MP4 slayd-shou video yig'asiz.

## Nima qila oladi

- **Aktyorlar kutubxonasi** — istagancha aktyor qo'shiladi. Har biriga 1–4 ta surat yuklanadi,
  ilova shu suratlardan inglizcha "yuz pasporti" (face reference sheet) yozadi.
- **Senariy** — g'oyadan o'zbek tilida sarlavha, logline, sahnalar, dialog va kamera ko'rsatmalari.
- **Sahnalashtirish** — har bir sahna uchun rasm prompti avtomatik tayyorlanadi (tahrirlash mumkin).
- **Yuz izchilligi** — har bir kadr so'rovi bilan birga aktyorning etalon surati va yuz pasporti
  yuboriladi, prompt ichida "identity lock" yo'riqnomasi bo'ladi. Shu sababli bir personaj
  barcha sahnalarda bir xil yuz bilan chiqadi.
- **Eksport** — rasmlar `Pictures/Kodava/...`, video `Movies/Kodava`, senariy `Documents/Kodava`
  papkalariga saqlanadi; alohida kadrni ulashish ham mumkin.

## APK'ni qanday olish

1. GitHub'da shu repozitoriyning **Actions** bo'limiga kiring.
2. Oxirgi **Build APK** ishini oching.
3. **Artifacts** ichidan `kodava-release-apk` (yoki `kodava-debug-apk`) ni yuklab oling.
4. ZIP ichidagi `.apk` faylni telefonga ko'chirib o'rnating
   (Sozlamalar → "Noma'lum manbalardan o'rnatishga ruxsat").

> APK debug kalit bilan imzolanadi — telefonga to'g'ridan-to'g'ri o'rnatiladi, Play Store talab qilinmaydi.

## API kalit

Ilova Google Gemini API bilan ishlaydi (matn + rasm).

1. https://aistudio.google.com → **Get API key**.
2. Ilovada **Sozlamalar** → kalitni qo'ying → **Saqlash**.

Kalit faqat telefon ichida (`SharedPreferences`) saqlanadi va so'rovlar to'g'ridan-to'g'ri
Google API'siga ketadi — oraliq server yo'q.

Standart modellar (Sozlamalarda o'zgartirsa bo'ladi):

| Vazifa | Model |
| --- | --- |
| Senariy (matn) | `gemini-2.5-flash` |
| Sahna kadri (rasm) | `gemini-2.5-flash-image` |

## Ishlash tartibi

1. **Aktyorlar** → aktyor qo'shing, surat yuklang, **Yuz tavsifini yaratish** tugmasini bosing.
2. **Yangi loyiha** → g'oyani yozing, uslub va kadr nisbatini tanlang, aktyorlarni belgilang.
3. **Senariy yaratish** → sahnalar ro'yxati paydo bo'ladi.
4. **Sahnalarni chizish** → barcha kadrlar ketma-ket yaratiladi (yoki har birini alohida).
5. **Video yig'ish** / **Rasmlarni saqlash** → natijani telefoningizga oling.

Yuz mos kelmasa: aktyorning suratini yuzi aniqroq ko'ringan rasm bilan almashtiring,
yuz pasportini qayta yarating va kadrni **Qayta chizish** tugmasi bilan yangilang.

## Texnik tafsilotlar

- Kotlin + Jetpack Compose (Material 3), minSdk 29, targetSdk 35.
- Ma'lumotlar telefon ichida JSON + JPG sifatida saqlanadi, hisob/registratsiya yo'q.
- Video: `MediaCodec` (H.264) + `MediaMuxer` bilan yig'iladi, sahnalar orasida crossfade;
  tashqi kutubxona yoki ffmpeg ishlatilmaydi.
- Rasm so'rovlari `generativelanguage.googleapis.com` ga OkHttp orqali yuboriladi;
  model versiyalari farqiga qarshi so'rov avtomatik soddalashtirilib qayta yuboriladi.

## Loyihani lokal yig'ish

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Android SDK va JDK 17 kerak.
