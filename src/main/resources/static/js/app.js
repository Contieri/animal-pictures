"use strict";

/* Snuggle Post front end: order form, gallery with tabs and paging, full-size viewer. */

const ANIMALS = {
  cat: { singular: "cat", plural: "cats", title: "Cat", icon: "img/cat.png" },
  dog: { singular: "dog", plural: "dogs", title: "Dog", icon: "img/dog.svg" },
  bear: { singular: "bear", plural: "bears", title: "Bear", icon: "img/bear.png" }
};

/** The little illustration of an animal, used as decoration (screen readers skip it). */
function animalIcon(animal, className) {
  const img = document.createElement("img");
  img.src = ANIMALS[animal].icon;
  img.alt = "";
  img.className = className;
  return img;
}
const GALLERY_PAGE_SIZE = 12;
const dateFormat = new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" });
const shortDateFormat = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" });

const $ = (id) => document.getElementById(id);

/* ---------- API ---------- */

async function api(path, options) {
  let response;
  try {
    response = await fetch(path, options);
  } catch {
    throw new Error("We could not reach Snuggle Post. Check that the app is running and try again.");
  }
  if (!response.ok) {
    let message = response.statusText;
    try {
      const problem = await response.json();
      message = problem.detail || problem.title || message;
    } catch { /* not JSON */ }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

/* ---------- Helpers ---------- */

function plural(animal, n) {
  return n === 1 ? ANIMALS[animal].singular : ANIMALS[animal].plural;
}

function formatSize(bytes) {
  return bytes < 1024 * 1024 ? Math.round(bytes / 1024) + " KB" : (bytes / 1024 / 1024).toFixed(1) + " MB";
}

function formatType(contentType) {
  return contentType.replace("image/", "").toUpperCase();
}

function setStatus(text, kind = "") {
  const status = $("status");
  status.className = "status" + (kind ? " is-" + kind : "");
  status.replaceChildren();
  if (kind === "loading") {
    const spinner = document.createElement("span");
    spinner.className = "spinner";
    spinner.setAttribute("aria-hidden", "true");
    status.append(spinner);
  }
  status.append(document.createTextNode(text));
}

function setBusy(button, busy) {
  button.disabled = busy;
  button.classList.toggle("is-loading", busy);
}

function skeletons(count, animal) {
  return Array.from({ length: count }, () => {
    const card = document.createElement("div");
    card.className = "skeleton";
    card.setAttribute("aria-hidden", "true");
    const frame = document.createElement("div");
    frame.className = "skeleton-frame";
    if (animal) frame.append(animalIcon(animal, "skeleton-icon"));
    const line = document.createElement("div");
    line.className = "skeleton-line";
    card.append(frame, line);
    return card;
  });
}

/** A clickable postcard for one picture. `list` and `index` let the viewer page through its neighbours. */
function photoCard(picture, list, index, isNew = false) {
  const card = document.createElement("button");
  card.type = "button";
  card.className = "photo-card" + (isNew ? " is-new" : "");
  if (isNew) card.style.animationDelay = (index * 70) + "ms";
  card.setAttribute("aria-label", `Open ${ANIMALS[picture.animal].singular} picture saved ${dateFormat.format(new Date(picture.createdAt))}`);

  const frame = document.createElement("div");
  frame.className = "photo-frame";
  const img = document.createElement("img");
  img.alt = "";
  img.loading = "lazy";
  img.decoding = "async";
  img.addEventListener("load", () => img.classList.add("is-loaded"));
  img.src = picture.imageUrl;
  frame.append(img);

  const info = document.createElement("div");
  info.className = "photo-info";
  const tag = document.createElement("span");
  tag.className = "tag tag-" + picture.animal;
  tag.textContent = ANIMALS[picture.animal].title;
  const date = document.createElement("span");
  date.className = "photo-date";
  date.textContent = shortDateFormat.format(new Date(picture.createdAt));
  info.append(tag, date);

  card.append(frame, info);
  card.addEventListener("click", () => viewer.open(list, index));
  return card;
}

/* ---------- Order form ---------- */

const form = $("order-form");
const countInput = $("count");
const resultsGrid = $("results-grid");
const sendButton = $("send-btn");
const latestButton = $("latest-btn");

function selectedAnimal() {
  return form.elements.animal.value;
}

function clampCount(value) {
  const n = Number.parseInt(value, 10);
  return Number.isNaN(n) ? 1 : Math.min(10, Math.max(1, n));
}

$("count-minus").addEventListener("click", () => { countInput.value = clampCount(countInput.value) - 1 || 1; });
$("count-plus").addEventListener("click", () => { countInput.value = clampCount(Number(countInput.value) + 1); });
countInput.addEventListener("change", () => { countInput.value = clampCount(countInput.value); });

form.addEventListener("change", (event) => {
  if (event.target.name === "animal") $("latest-animal").textContent = ANIMALS[selectedAnimal()].singular;
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const animal = selectedAnimal();
  const count = clampCount(countInput.value);
  countInput.value = count;

  setBusy(sendButton, true);
  latestButton.disabled = true;
  resultsGrid.setAttribute("aria-busy", "true");
  resultsGrid.replaceChildren(...skeletons(count, animal));
  setStatus(`Fetching ${count} ${plural(animal, count)} for you…`, "loading");

  try {
    const pictures = await api(`/api/pictures/${animal}?count=${count}`, { method: "POST" });
    resultsGrid.replaceChildren(...pictures.map((p, i) => photoCard(p, pictures, i, true)));
    setStatus(`Here ${pictures.length === 1 ? "is your" : "are your"} ${pictures.length} ${plural(animal, pictures.length)}. Click one to see it full size.`, "success");
    refreshCounts();
    gallery.reload();
  } catch (error) {
    resultsGrid.replaceChildren();
    setStatus(error.message, "error");
  } finally {
    setBusy(sendButton, false);
    latestButton.disabled = false;
    resultsGrid.setAttribute("aria-busy", "false");
  }
});

latestButton.addEventListener("click", async () => {
  const animal = selectedAnimal();
  latestButton.disabled = true;
  resultsGrid.setAttribute("aria-busy", "true");
  resultsGrid.replaceChildren(...skeletons(1, animal));
  setStatus(`Finding your latest ${animal}…`, "loading");
  try {
    const picture = await api(`/api/pictures/${animal}/latest/metadata`);
    resultsGrid.replaceChildren(photoCard(picture, [picture], 0, true));
    setStatus(`Your latest ${animal}, saved ${dateFormat.format(new Date(picture.createdAt))}.`, "success");
  } catch (error) {
    resultsGrid.replaceChildren();
    setStatus(error.status === 404 ? `You have no ${plural(animal, 2)} yet. Send yourself some comfort first!` : error.message,
      error.status === 404 ? "" : "error");
  } finally {
    latestButton.disabled = false;
    resultsGrid.setAttribute("aria-busy", "false");
  }
});

/* ---------- Counts ---------- */

async function refreshCounts() {
  try {
    const counts = await api("/api/pictures/counts");
    $("count-total").textContent = counts.total;
    $("stat-total").textContent = counts.total;
    for (const animal of Object.keys(ANIMALS)) $("count-" + animal).textContent = counts[animal] ?? 0;
  } catch { /* counts are a nice-to-have */ }
}

/* ---------- Gallery ---------- */

const gallery = (() => {
  const grid = $("gallery-grid");
  const empty = $("gallery-empty");
  const loadMore = $("load-more");
  const tabs = Array.from(document.querySelectorAll(".tab"));
  let animal = "";
  let page = 0;
  let totalPages = 0;
  let items = [];
  let requestId = 0;

  async function load(reset) {
    const id = ++requestId;
    if (reset) {
      page = 0;
      items = [];
      grid.replaceChildren(...skeletons(6, animal || null));
      grid.setAttribute("aria-busy", "true");
      empty.hidden = true;
      loadMore.hidden = true;
    } else {
      setBusy(loadMore, true);
    }

    try {
      const query = new URLSearchParams({ page, size: GALLERY_PAGE_SIZE });
      if (animal) query.set("animal", animal);
      const result = await api("/api/pictures?" + query);
      if (id !== requestId) return; // a newer tab was chosen meanwhile

      const start = items.length;
      items = items.concat(result.items);
      totalPages = result.totalPages;
      const cards = result.items.map((p, i) => photoCard(p, items, start + i));
      if (reset) grid.replaceChildren(...cards); else grid.append(...cards);

      empty.hidden = items.length > 0;
      if (!items.length) {
        empty.textContent = animal
          ? `No ${plural(animal, 2)} in your collection yet. Choose ${animal === "bear" ? "a bear" : "a " + animal} above and send yourself some comfort.`
          : "Your collection is empty for now. Your first pictures will appear here.";
      }
      loadMore.hidden = page + 1 >= totalPages;
    } catch (error) {
      if (id !== requestId) return;
      if (reset) grid.replaceChildren();
      empty.hidden = false;
      empty.textContent = "We could not load your collection: " + error.message;
    } finally {
      if (id === requestId) {
        grid.setAttribute("aria-busy", "false");
        setBusy(loadMore, false);
      }
    }
  }

  tabs.forEach((tab) => tab.addEventListener("click", () => {
    tabs.forEach((t) => t.setAttribute("aria-selected", String(t === tab)));
    animal = tab.dataset.animal;
    load(true);
  }));

  loadMore.addEventListener("click", () => { page += 1; load(false); });

  return { reload: () => load(true) };
})();

/* ---------- Full-size viewer ---------- */

const viewer = (() => {
  const dialog = $("viewer");
  const img = $("viewer-img");
  const loading = $("viewer-loading");
  const prev = $("viewer-prev");
  const next = $("viewer-next");
  let list = [];
  let index = 0;

  function show() {
    const picture = list[index];
    const animal = ANIMALS[picture.animal];
    img.hidden = true;
    loading.hidden = false;
    img.alt = `A ${animal.singular}, picture #${picture.id}`;
    img.src = picture.imageUrl;

    $("viewer-title").replaceChildren(animalIcon(picture.animal, "viewer-icon"), `${animal.title} #${picture.id}`);
    $("viewer-date").textContent = dateFormat.format(new Date(picture.createdAt));
    $("viewer-size").textContent = formatSize(picture.sizeBytes);
    $("viewer-format").textContent = formatType(picture.contentType);
    const source = $("viewer-source");
    source.href = picture.sourceUrl;
    source.textContent = picture.sourceUrl.replace(/^https?:\/\//, "");
    $("viewer-position").textContent = list.length > 1 ? `Picture ${index + 1} of ${list.length} · use ← → to browse` : "";

    prev.hidden = next.hidden = list.length < 2;
    prev.disabled = index === 0;
    next.disabled = index === list.length - 1;
  }

  img.addEventListener("load", () => {
    loading.hidden = true;
    img.hidden = false;
    const picture = list[index];
    $("viewer-size").textContent = `${formatSize(picture.sizeBytes)} · ${img.naturalWidth} × ${img.naturalHeight} px`;
  });
  img.addEventListener("error", () => {
    loading.hidden = false;
    loading.textContent = "This picture could not be loaded.";
  });

  function go(step) {
    const target = index + step;
    if (target >= 0 && target < list.length) {
      index = target;
      show();
    }
  }

  prev.addEventListener("click", () => go(-1));
  next.addEventListener("click", () => go(1));
  $("viewer-close").addEventListener("click", () => dialog.close());
  dialog.addEventListener("click", (event) => { if (event.target === dialog) dialog.close(); });
  dialog.addEventListener("keydown", (event) => {
    if (event.key === "ArrowLeft") go(-1);
    if (event.key === "ArrowRight") go(1);
  });

  return {
    open(pictures, start) {
      list = pictures;
      index = start;
      show();
      if (!dialog.open) dialog.showModal();
    }
  };
})();

/* ---------- Start ---------- */

refreshCounts();
gallery.reload();
