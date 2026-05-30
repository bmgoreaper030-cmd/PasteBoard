# PasteBoard Android — GitHub Setup Guide

## What you need
- A GitHub account (you have one ✅)
- Your Android phone
- ~5 minutes

---

## Step 1 — Create a new GitHub repo

1. Go to **github.com** → click the **+** icon top right → **New repository**
2. Name it: `PasteBoard`
3. Set to **Public**
4. Leave everything else as default
5. Click **Create repository**

---

## Step 2 — Upload all the files

You have two options:

### Option A — GitHub Web (easiest, no Git needed)
1. On your new repo page, click **uploading an existing file**
2. Drag and drop the entire `PasteBoardAndroid` folder contents
3. **Important:** Keep the folder structure exactly as-is
4. Scroll down → click **Commit changes**

### Option B — Git command line
```bash
cd PasteBoardAndroid
git init
git remote add origin https://github.com/YOURUSERNAME/PasteBoard.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

---

## Step 3 — Watch GitHub build your APK

1. Go to your repo on GitHub
2. Click the **Actions** tab at the top
3. You'll see **"Build APK"** workflow running (yellow spinner)
4. Wait ~3-5 minutes for it to finish (turns green ✅)
5. Click on the completed run → scroll down to **Artifacts**
6. Click **PasteBoard-APK** to download a zip
7. Unzip it → you'll have `app-debug.apk`

---

## Step 4 — Install on your Android phone

### Enable installing unknown apps:
1. Settings → Apps → Special app access → Install unknown apps
2. Find your browser or Files app → enable **Allow from this source**

### Install the APK:
1. Transfer `app-debug.apk` to your phone (AirDrop equivalent: email it, Google Drive, or USB)
2. Open the APK file on your phone
3. Tap **Install**

---

## Step 5 — Enable the keyboard

1. Open **PasteBoard** app
2. Tap **Enable Keyboard in Settings**
3. Find **PasteBoard** in the list → toggle it ON
4. In any app, tap a text field → long press the keyboard icon (🌐 or space bar) → select **PasteBoard**

---

## Using the keyboard

- Import `.txt` files in the main app (one line = one paste)
- Tap **Paste Line** to insert the current line
- Tap 🔀 to toggle shuffle mode
- Tap ☀️/🌙 to toggle dark/light theme
- Tap ➡️ to switch between your loaded script files
- Tap 🌐 to switch back to your normal keyboard

---

## .txt file format
```
Hey! How's it going?
Thanks for the message!
I'll get back to you soon.
Sounds good, let's connect!
```
One line per paste. Blank lines are ignored.
