# 📚 Komga API Integration Guide

The Komga API allows you to access your Booklore library using popular manga and comic reading applications that support the Komga server standard. This guide will walk you through setting up the Komga API and connecting your favorite reading apps to your Booklore collection.

---

## 🔧 What is the Komga API?

[Komga](https://komga.org/) is a popular media server for comics and manga. Booklore provides a Komga-compatible API layer that enables Komga clients to access your Booklore library. With the Komga API enabled in Booklore, you can:

- Browse your library from any Komga-compatible reading app
- Read comics and manga directly on your mobile device
- Access your collection from multiple devices seamlessly
- Use apps like Mihon (formerly Tachiyomi), Komelia, Tachidesk, and many others
- Stream individual pages instead of downloading entire files

---

## ⚙️ Step 1: Enable the Komga API

First, let's enable the Komga API in your Booklore settings.

1. Navigate to **Settings** > **OPDS** in Booklore
2. Under **Komga API**, toggle **Komga API Enabled** to activate the service

:::info[Note]
The Komga API shares authentication with the OPDS server and uses OPDS user accounts. You'll need to enable OPDS users to authenticate with Komga clients.
:::

---

## 🔗 Step 2: Get Your Komga API Base URL

Booklore provides the Komga API at a dedicated endpoint:

### Komga Base URL

- **URL:** `http://localhost:8080/komga`
- **API Endpoints:** `/komga/api/v1/*`
- **Compatibility:** Works with Komga-compatible reading apps
- **Authentication:** Requires OPDS user credentials (see Step 3)

You can copy this URL directly from the **Komga API Endpoint** section in your settings.

:::tip[Network Access]
Replace `localhost:8080` with your actual Booklore server address when accessing from other devices on your network.
:::

---

## 👤 Step 3: Create OPDS Users for Authentication

The Komga API uses OPDS user credentials for authentication. To access your Komga API, you'll need to create dedicated OPDS user credentials.

1. In the **OPDS Users** section, click **Add User**
2. Fill in the user details:

| Field        | Description                                 |
|--------------|---------------------------------------------|
| **Username** | Choose a username for API access            |
| **Password** | Create a strong password for this user      |

3. Click **Create** to create the OPDS user

:::danger[Security Note]
OPDS users are separate from your main Booklore account and are specifically for reading app access. Make sure OPDS is enabled to use these credentials with the Komga API.
:::

:::warning[Important]
Passwords are created once and cannot be retrieved later. Make sure to store your OPDS credentials securely before saving.
:::

---

## ⚙️ Step 4: Configure Group Unknown Series (Optional)

Booklore organizes books differently than Komga:

- **Komga**: Libraries → Series → Books
- **Booklore**: Libraries → Books (with optional series metadata)

The Komga API automatically creates virtual "series" by grouping books with the same series name in their metadata.

### Group Unknown Series Feature

When enabled, books without series metadata are grouped together:

- **Enabled (Default)**: Books without a series are grouped under a single "Unknown Series" entry
- **Disabled**: Each book without a series appears as its own individual series

To configure this setting:

1. Navigate to **Settings** > **OPDS** in Booklore
2. Under **Komga API**, toggle **Group Unknown Series** to your preference

:::tip[Recommendation]
Keep this enabled if you have many standalone books to avoid cluttering your series list. Disable it if you want to see each standalone book separately.
:::

---

## 📱 Step 5: Connect Reading Apps

Now that your Komga API is configured, you can connect various reading apps to access your Booklore library.

### Popular Komga-Compatible Apps:

- **Mihon** (Android) - Formerly known as Tachiyomi, a popular manga reader
- **Komelia** (Multiple platforms) - Official Komga client
- **Tachidesk** (Desktop) - Web-based manga reader with Komga support
- **TachiyomiSY** (Android) - Tachiyomi fork with additional features
- **TachiyomiJ2K** (Android) - Another Tachiyomi fork

### General Connection Steps:

1. Open your reading app
2. Look for **Add Source**, **Add Server**, or **Browse Sources** settings
3. Find and select the **Komga** source/extension
4. Configure the Komga source:
   - **Server URL:** `http://your-booklore-domain/komga`
   - **Username:** Your OPDS username
   - **Password:** Your OPDS password
5. Save and browse your library

---

## 🔄 Example: Connecting Mihon (formerly Tachiyomi)

Mihon is one of the most popular manga reading apps for Android with excellent Komga support.

### Installing the Komga Extension

1. Open Mihon
2. Tap **Browse** at the bottom navigation
3. Tap **Extensions** tab
4. Find **Komga** in the list (or search for it)
5. Tap **Install** next to the Komga extension
6. Wait for the extension to install

### Configuring the Komga Source

1. After installation, go back to the **Sources** tab in Browse
2. Tap the **Komga** source
3. Tap the settings icon (gear icon) in the top right
4. Enter your connection details:
   - **Address:** `http://your-booklore-domain/komga`
   - **Username:** Your OPDS username
   - **Password:** Your OPDS password
5. Tap **OK** or **Save**
6. Go back and tap **Komga** again to browse your libraries

### Using Mihon with Booklore

- **Browse Libraries:** See all your Booklore libraries
- **Browse Series:** View books grouped by series
- **Read Online:** Stream pages without downloading entire books
- **Download:** Download books for offline reading
- **Track Progress:** Keep track of what you've read

:::tip[Mihon Tips]
- Use the search function to quickly find books
- Long-press on a series to add it to your library for quick access
- Enable auto-tracking in settings to remember your reading progress
:::

---

## 🔄 Example: Connecting Komelia

Komelia is the official Komga client with cross-platform support.

1. Open Komelia
2. Click **Add Server** or the **+** button
3. Enter your server details:
   - **Server URL:** `http://your-booklore-domain/komga`
   - **Username:** Your OPDS username
   - **Password:** Your OPDS password
4. Click **Connect** or **Add**
5. Browse your Booklore libraries and series

:::info[Komelia Features]
Komelia provides a desktop and mobile experience optimized for the Komga API with features like collection management, advanced filtering, and read progress sync.
:::

---

## 📖 Komga API Features

The Booklore Komga API implementation provides comprehensive endpoints for accessing your library:

### Available Endpoints

#### Libraries
- **List all libraries** - `GET /komga/api/v1/libraries`
- **Get library details** - `GET /komga/api/v1/libraries/{libraryId}`

#### Series
- **List series** - `GET /komga/api/v1/series`
  - Supports pagination (`page`, `size`)
  - Library filtering (`library_id`)
  - Unpaged mode (`unpaged=true`)
- **Get series details** - `GET /komga/api/v1/series/{seriesId}`
- **List books in series** - `GET /komga/api/v1/series/{seriesId}/books`
- **Get series thumbnail** - `GET /komga/api/v1/series/{seriesId}/thumbnail`

#### Books
- **List all books** - `GET /komga/api/v1/books`
  - Supports pagination (`page`, `size`)
  - Library filtering (`library_id`)
- **Get book details** - `GET /komga/api/v1/books/{bookId}`
- **Get book pages metadata** - `GET /komga/api/v1/books/{bookId}/pages`
- **Get book page image** - `GET /komga/api/v1/books/{bookId}/pages/{pageNumber}`
  - Supports format conversion (`convert=png`)
- **Download book file** - `GET /komga/api/v1/books/{bookId}/file`
- **Get book thumbnail** - `GET /komga/api/v1/books/{bookId}/thumbnail`

#### Collections
- **List collections** - `GET /komga/api/v1/collections`
  - Maps to Booklore Magic Shelves
  - Supports pagination

#### Users
- **Get current user** - `GET /komga/api/v2/users/me`

### Clean Mode

All Komga API endpoints support a `clean` query parameter for smaller responses:

- **Usage:** Add `?clean` or `?clean=true` to any endpoint
- **Effect:** Removes null values, empty arrays, and lock fields
- **Benefit:** Significantly smaller JSON payloads, ideal for mobile devices

**Example:**
```
GET /komga/api/v1/series?clean
GET /komga/api/v1/books/123?clean=true
```

For more details, see the [Komga API Clean Mode](komga-clean-mode.md) documentation.

---

## 🔍 Troubleshooting

### Common Issues and Solutions

**❌ Reading App Can't Connect**

- Verify the Komga API is enabled in Booklore settings
- Check that you're using the correct base URL: `http://your-server/komga`
- Ensure your OPDS username and password are correct
- Verify network connectivity to your Booklore server
- Make sure OPDS is enabled (required for authentication)

**❌ Books Not Appearing**

- Confirm you have access to the libraries containing the books
- Check that books have metadata populated, especially series information
- Books without series metadata will appear under "Unknown Series" (if grouping is enabled)
- Refresh or restart your reading app
- Try re-syncing the source in your app

**❌ Authentication Errors**

- The Komga API uses OPDS user accounts, not your main Booklore account
- Create an OPDS user in the Settings → OPDS section
- Verify the OPDS user exists in Booklore settings
- Try creating a new OPDS user if issues persist
- Ensure OPDS server is enabled

**❌ Series Show as "Unknown Series"**

- This means the books don't have series metadata populated
- Edit book metadata in Booklore to add series information
- Alternatively, disable "Group Unknown Series" to show each book separately

**❌ Pages Not Loading**

- Verify the book file is accessible
- Check that the book is a supported format (CBZ, CBR, CB7, PDF, EPUB)
- Ensure your reading app supports streaming pages
- Try downloading the entire book instead of streaming

---

## 🌐 Network Considerations

### Local Network Access

:::info[Local Network Access]
Replace `localhost:8080` with your actual Booklore server IP address when connecting from other devices on your local network.

Example: `http://192.168.1.100:8080/komga`
:::

### Remote Access

:::info[Remote Access]
Ensure your Booklore server is accessible from the internet. Use HTTPS for secure connections when accessing remotely.

Example: `https://booklore.yourdomain.com/komga`
:::

### Firewall Settings

:::warning[Firewall Settings]
Ensure your Booklore server port is accessible and your firewall allows Komga API connections on the appropriate port.
:::

---

## 🆚 Komga API vs OPDS

Both APIs allow external access to your Booklore library, but they serve different purposes:

| Feature | Komga API | OPDS |
|---------|-----------|------|
| **Primary Use** | Manga/comic readers | E-book readers |
| **Page Streaming** | ✅ Yes | ❌ No |
| **Series Organization** | ✅ Native support | ⚠️ Limited |
| **File Download** | ✅ Yes | ✅ Yes |
| **Popular Apps** | Mihon, Komelia | KOReader, FBReader |
| **Best For** | Comics, manga, visual novels | Books, novels, text content |

:::tip[When to Use Which]
- Use **Komga API** for comics, manga, and graphic novels where you want to stream pages
- Use **OPDS** for traditional e-books (EPUB, MOBI, PDF) where you download entire files
- Both use the same OPDS user credentials for authentication
:::

---

## 📋 API Compatibility

The Booklore Komga API aims to be compatible with Komga v1.x API. While not all endpoints are implemented, the core functionality needed for reading and browsing is fully supported.

### Supported Features:
- ✅ Library browsing
- ✅ Series browsing and filtering
- ✅ Book metadata retrieval
- ✅ Page streaming
- ✅ Thumbnail retrieval
- ✅ Book file downloads
- ✅ Basic authentication
- ✅ Pagination
- ✅ Clean mode responses

### Known Limitations:
- ⚠️ Read progress tracking from Komga clients is not synchronized with Booklore
- ⚠️ Some advanced Komga features may not be implemented
- ⚠️ Collection support is mapped to Magic Shelves (limited functionality)

For the complete Komga API specification, see: [Komga Documentation](https://komga.org/)

---

## 🎯 Best Practices

1. **Use Strong Passwords**: Create secure passwords for your OPDS users
2. **Enable HTTPS**: Use secure connections when accessing remotely
3. **Organize Your Library**: Add series metadata to books for better organization
4. **Keep Apps Updated**: Update your reading apps regularly for best compatibility
5. **Test Locally First**: Verify connections work on your local network before configuring remote access
