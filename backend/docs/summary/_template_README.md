# HTML Documentation Templates

This directory contains reusable HTML templates for generating Klabis documentation pages.

## Files

### `_template_base.html`

Complete base template with all CSS styles and common page structure. Use this as the starting point for any new
documentation page.

### `_template_components.html`

Collection of reusable HTML components that can be copied and pasted into pages. Includes:

- Breadcrumb
- Hero section
- Highlight box
- Feature grid
- Event/Token flow
- API sections
- Diagram boxes
- Cards (for index page)
- Stats (for index page)

## Usage

### Creating a New Page

1. Copy `_template_base.html` to your new page file (e.g., `new-feature.html`)

2. Replace placeholders:
    - `{{TITLE}}` - Page title (in `<title>` tag)
    - Add `active` class to the correct navigation link
    - Replace `<!-- BREADCRUMB_PLACEHOLDER -->` with breadcrumb component
    - Replace `<!-- HERO_PLACEHOLDER -->` with hero component
    - Replace `<!-- CONTENT_PLACEHOLDER -->` with your content

3. Use components from `_template_components.html` to build content

### Page Structure

```html
<!DOCTYPE html>
<html lang="cs">
<head>
    <!-- Head with CSS styles -->
</head>
<body>
    <div class="container">
        <nav class="sidebar">
            <!-- Navigation - mark current page with 'active' class -->
        </nav>

        <main class="main-content">
            <!-- Breadcrumb -->
            <div class="breadcrumb">
                <a href="index.html">Úvodní stránka</a> / Page Title
            </div>

            <!-- Hero -->
            <div class="hero">
                <h2>Page Title</h2>
                <p class="lead">Description...</p>
            </div>

            <!-- Optional: Highlight Box -->
            <div class="highlight-box">
                <h4>Box Title</h4>
                <p>Box content...</p>
            </div>

            <!-- Sections -->
            <div class="section-title">Section Title</div>

            <!-- Content: Feature Grid, API Section, Flow, etc. -->
            <div class="feature-grid">
                <!-- Features -->
            </div>
        </main>
    </div>
</body>
</html>
```

## CSS Custom Properties

Colors can be customized via CSS variables in `:root`:

```css
--navy: #0A1628;
--navy-light: #1A2B45;
--cyan: #00D9FF;
--cyan-dark: #00A8CC;
--cream: #FAF8F5;
--cream-dark: #F0EDE8;
--gray: #6B7280;
--gray-light: #E5E7EB;
--white: #FFFFFF;
```

## Common Page Types

### Feature Specification Page

- Breadcrumb
- Hero with title and description
- Highlight box with key feature
- Multiple section-title + feature-grid sections
- Optional: event-flow or token-flow

### Index Page

- Hero (larger h2: 48px)
- cards-grid for overview cards
- stats section at bottom

## Design Guidelines

1. **Language**: All content in Czech (cs)
2. **Colors**: Use navy for backgrounds, cyan for accents/highlights
3. **Typography**: IBM Plex Sans for body, IBM Plex Mono for code/endpoints
4. **Spacing**: Use section-title to create visual separation (50px top margin)
5. **Links**: Always include breadcrumb with link back to index.html

## Component Reference

| Component        | Class                                         | Description |
|------------------|-----------------------------------------------|-------------|
| `.highlight-box` | Info box with cyan left border                |
| `.feature-grid`  | Grid of feature cards                         |
| `.feature-item`  | Individual feature card                       |
| `.event-flow`    | Horizontal flow with numbered steps           |
| `.token-flow`    | Same as event-flow                            |
| `.api-section`   | Dark background for API docs                  |
| `.api-endpoint`  | Single API endpoint                           |
| `.method`        | HTTP method badge (get/post/put/patch/delete) |
| `.diagram-box`   | Dark background for architecture diagrams     |
| `.card`          | Large card for index page                     |
| `.stats`         | Stats container (index page)                  |
