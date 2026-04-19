# HTML konvence

Manuál používá čisté sémantické HTML5 bez inline stylů. Veškerá vizuální úprava je v `style.css` (kopie v `assets/style.css` v tomto skillu).

## Šablona stránky

Každá stránka má identickou kostru:

```html
<!DOCTYPE html>
<html lang="cs">
<head>
<meta charset="UTF-8">
<title>Název kapitoly — Klabis Developer Manual</title>
<link rel="stylesheet" href="style.css">
</head>
<body>

<header class="page-header">
    <a href="index.html">Developer Manual</a>
    <span class="sep">›</span>
    <span>NN · Název kapitoly</span>
</header>

<main class="page-main">
<article>

<h1>Název kapitoly</h1>

<p class="lead">
    Krátký úvodní odstavec — co kapitola pokrývá a proč.
</p>

<h2>...</h2>
...

</article>
</main>

<nav class="page-nav">
    <a href="NN-prev.html">← NN-1 · Předchozí</a>
    <a href="NN-next.html">NN+1 · Další →</a>
</nav>

</body>
</html>
```

## Vocabulary nestandardních tříd

Tyto třídy musí všichni vývojáři manuálu používat konzistentně. Veškeré CSS pro ně je v `style.css`. Pokud přidáš novou třídu, přidej ji do `style.css` (komentář na začátku) i sem.

### `.lead` — úvodní odstavec
Větší/tlumenější text na začátku kapitoly, jeden odstavec shrnutí.

```html
<p class="lead">Co tato kapitola pokrývá a proč.</p>
```

### `<aside class="note">` a `<aside class="note warn">` — poznámka
Informativní (neutrální) nebo varovná (jantarová) poznámka.

```html
<aside class="note">Doplňující informace, kontext.</aside>
<aside class="note warn">Důležité upozornění, gotcha.</aside>
```

### `<dl class="api-list">` — definice API
Seznam položek API s krátkým popisem. Používej pro výčty metod, anotací, properties, services.

```html
<dl class="api-list">
    <dt>UserService</dt>
    <dd>Interface pro správu uživatelů. Vrací <code>UserId</code>.</dd>

    <dt>createUser(...)</dt>
    <dd>Vytvoří uživatele se statusem <code>PENDING_ACTIVATION</code>.</dd>
</dl>
```

### `<pre class="signature">` — code signature
Krátký Java/TypeScript snippet (5-15 řádků). Pouze interface/anotace/deklarace, ne implementace. Generic typy escapuj `&lt;`/`&gt;`.

```html
<pre class="signature"><code>@PrimaryPort
public interface UserService {
    UserId createUser(String username, String email, Set&lt;Authority&gt; authorities);
}</code></pre>
```

### `<code class="path">` — cesta k souboru
Inline cesta v repozitáři. Tlumeně, bez pozadí.

```html
<code class="path">backend/src/main/java/com/klabis/common/email/EmailService.java</code>
```

### `<table class="ref-table">` — referenční tabulka
Pro přehled enum hodnot, mapování, srovnání.

```html
<table class="ref-table">
    <thead><tr><th>Sloupec</th><th>Význam</th></tr></thead>
    <tbody>
        <tr><td>...</td><td>...</td></tr>
    </tbody>
</table>
```

### `<ul class="feature-list">` — seznam features
Pro tematické bloky s `<strong>` nadpisem položky.

```html
<ul class="feature-list">
    <li><strong>KlabisAggregateRoot</strong> — base třída pro agregáty.</li>
</ul>
```

### `<a class="ext">` — externí odkaz
Pro odkazy na oficiální dokumentaci. Otevírají se v nové záložce.

```html
<a class="ext" href="https://docs.spring.io/spring-boot/" target="_blank" rel="noopener">Spring Boot</a>
```

### `.page-header`, `.page-main`, `.page-nav` — layout
Top-level layout třídy. Viz šablona výše.

### `.toc` — obsah na index.html
Pouze na `index.html` — strukturovaný seznam stránek.

```html
<ol class="toc">
    <li>
        <span class="num">NN</span>
        <a class="title" href="NN-name.html">Název</a>
        <span class="desc">Krátký popis.</span>
    </li>
</ol>
```

## HTML pravidla

- **Žádné inline `style=` atributy.** Všechno přes třídy.
- **HTML entity:** `&lt;`, `&gt;`, `&amp;`, `&quot;`. Generic typy v `<pre>` musí být escaped (`Set&lt;Authority&gt;`).
- **`&nbsp;` v textu mezi `&` a slovem** — vyhni se, používej jen v "&" oddělovačích např. v titulu (`Domain events &amp; email`).
- **Inline kód:** `<code>...</code>` (s pozadím). Pro cesty `<code class="path">`.
- **Hlavičky:** `<h1>` jednou na stránku, `<h2>` pro sekce, `<h3>` pro pod-sekce. Čísla nepoužívej v textu (`<h2>1. Foo</h2>` ne, `<h2>Foo</h2>` ano — pořadí dává struktura).
- **Délka řádků:** kolem 100-120 znaků. Snadnější diff.
- **Citace tříd a metod:** v textu vždy v `<code>` (např. `<code>KlabisAggregateRoot</code>`).
