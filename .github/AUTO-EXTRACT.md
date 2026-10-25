# 📦 AVA — ZIP Auto Extract

এই repository-তে যেকোনো `.zip` আপলোড করলেই সেটি **স্বয়ংক্রিয়ভাবে extract** হয়ে যায় — কোনো হাতে কাজ নেই।

---

## ⚙️ কী ঘটে

| | |
|---|---|
| 📂 **যেখানে বসবে** | zip-এর ভেতরের ফাইলগুলো **সরাসরি repo root-এ** বসে (আলাদা `zip-নাম/` ফোল্ডার থাকে না) |
| ✂️ **একটাই ফোল্ডার থাকলে** | zip-এর ভেতরের একমাত্র ফোল্ডারটা (যেমন `AVA/`) খুলে ফেলা হয় — ফাইলগুলো flat হয়ে root-এ বসে |
| 🔄 **একই নামের ফাইল** | নতুন zip-এর ভার্সন দিয়ে **replace** হয় |
| 🗑️ **পুরোনো ফাইল** | আগের extraction-এ আসা কিন্তু নতুন zip-এ নেই এমন ফাইল **নিজে থেকেই মুছে যায়** (খালি ফোল্ডারও সরে যায়) |
| 📦 **zip ফাইলটি** | extract সফল হওয়ার পর **নিজে থেকেই মুছে যায়** |

### উদাহরণ

```
আপলোড:  AVA-AI-Companion.zip   (ভেতরে: AVA/app/…, AVA/build.gradle, …)
ফলাফল:  app/…, build.gradle, …   ← root-এ, zip মুছে গেছে
```

---

## 📬 কখন কী ঘটে

| ZIP কোথায় আসে | ফলাফল |
|---|---|
| রিপোজিটরিতে push / upload | repo root-এ extract হয় → ফাইলগুলো ব্রাঞ্চে commit হয় → zip মুছে যায় |
| Release-এ attach | `releases/<tag>/<name>/` এ extract হয় এবং `<name>-extracted.zip` হিসেবে release-এ আবার attach হয় |
| Actions → **Auto Extract ZIP** → *Run workflow* | ম্যানুয়ালি চালানো যায় (ঐচ্ছিক: সরাসরি `.zip` URL, আর zip রেখে দেওয়ার অপশন) |

---

## 📍 ফলাফল কোথায় পাবেন

- **Repository** — extract করা ফাইলগুলো ব্রাঞ্চে commit হয়ে যায়
- **Actions → run summary** — কোন ফাইল added / modified / deleted হলো তার তালিকা
- **Actions → Artifacts** — `extracted-files-<run number>` (30 দিন)

---

## 🛡️ সুরক্ষা

- `../` বা absolute path থাকা archive বাদ দেওয়া হয় (**zip-slip guard**) — এমন archive extract হয় না
- repo-র নিজস্ব ফাইলগুলো (zip-এর আগে থাকা) কখনো মুছে ফেলা হয় না
- অটোমেটেশনের নিজস্ব ফাইল (`auto-extract-zip.yml`, `extract-zips.sh`, manifest) সুরক্ষিত — নিজেই নিজেকে মুছতে পারে না
- auto-commit-এর মেসেজে `[skip actions]` থাকে, তাই কোনো rebuild লুপ হয় না

---

## 🧾 ফাইল কোথায় কী আছে

| ফাইল | কাজ |
|---|---|
| `.github/workflows/auto-extract-zip.yml` | workflow (push / release / manual ট্রিগার) |
| `.github/scripts/extract-zips.sh` | extraction লজিক, zip-slip গার্ড, stale ফাইল মুছে ফেলা |
| `.github/auto-extract-manifest.tsv` | শেষ extraction-এ কোন ফাইল এসেছে (পরের বার পুরোনো ফাইল সরাতে এটা লাগে) |
| `.github/auto-extract-baseline.txt` | repo-র নিজস্ব ফাইলের তালিকা (এগুলো সুরক্ষিত) |

---

*AVA — AI Companion | Developed by Fahim Khan*
