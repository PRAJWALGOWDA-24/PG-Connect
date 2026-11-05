const API = "/api";
const AMENITIES = ["wifi", "food", "ac", "laundry", "parking", "gym", "cctv"];
const COLLEGES = {
  "Pick a college (optional)": null,
  "Christ University": [12.9346, 77.6061],
  "IISc Bengaluru": [13.0219, 77.5671],
  "RV College of Engineering": [12.9237, 77.4998],
  "PES University (BSK)": [12.9345, 77.5345],
  "BMS College of Engineering": [12.9416, 77.5655],
  "Oxford College of Engineering": [12.8995, 77.6431],
};

let map, pin, circle, markers = {}, markerLayer;
let center = { lat: 12.9716, lng: 77.5946 };   // Bengaluru city centre
let currentList = [];

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/* ---------- setup ---------- */
function initMap() {
  map = L.map("map").setView([center.lat, center.lng], 12);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19, attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);
  markerLayer = L.layerGroup().addTo(map);
  pin = L.marker([center.lat, center.lng], { draggable: true }).addTo(map).bindTooltip("Search point (drag me)");
  circle = L.circle([center.lat, center.lng], { radius: 2000, color: "#0e7c86", weight: 1.5, fillOpacity: 0.06 }).addTo(map);

  map.on("click", (e) => setCenter(e.latlng.lat, e.latlng.lng));
  pin.on("dragend", () => { const p = pin.getLatLng(); setCenter(p.lat, p.lng, false); });
}

function setCenter(lat, lng, movePin = true) {
  center = { lat, lng };
  if (movePin) pin.setLatLng([lat, lng]);
  circle.setLatLng([lat, lng]);
  circle.setRadius(parseFloat($("radius").value) * 1000);
  $("wholeCity").checked = false;
  search();
}

function buildFilters() {
  $("college").innerHTML = Object.keys(COLLEGES).map(n => `<option>${esc(n)}</option>`).join("");
  $("amenityFilters").innerHTML = AMENITIES.map(a =>
    `<label class="chip"><input type="checkbox" value="${a}"><span>${a}</span></label>`).join("");
  $("f_amenities").innerHTML = $("amenityFilters").innerHTML;
}

/* ---------- search ---------- */
function filterParams() {
  const p = new URLSearchParams();
  if ($("gender").value) p.set("gender", $("gender").value);
  if ($("maxRent").value) p.set("maxRent", $("maxRent").value);
  const am = [...document.querySelectorAll("#amenityFilters input:checked")].map(i => i.value);
  if (am.length) p.set("amenities", am.join(","));
  return p;
}

async function search() {
  const params = filterParams();
  let url, list;
  try {
    if ($("wholeCity").checked) {
      url = `${API}/pgs?${params}`;
      list = (await (await fetch(url)).json()).map(pg => ({ pg, distanceKm: null }));
    } else {
      params.set("lat", center.lat); params.set("lng", center.lng);
      params.set("radius", $("radius").value);
      url = `${API}/pgs/nearby?${params}`;
      list = await (await fetch(url)).json();
    }
  } catch (e) {
    $("results").innerHTML = `<div class="empty">Could not reach the server. Check that the Spring Boot app is running.</div>`;
    return;
  }
  currentList = list;
  render(list);
}

function render(list) {
  markerLayer.clearLayers(); markers = {};
  $("resultCount").textContent = list.length ? `${list.length} PG${list.length > 1 ? "s" : ""} found` : "";

  if (!list.length) {
    $("results").innerHTML = `<div class="empty">No PGs match. Try a bigger distance, a higher rent limit or fewer amenities.</div>`;
    return;
  }
  $("results").innerHTML = list.map(({ pg, distanceKm }) => `
    <article class="pg" data-id="${pg.id}">
      <h3>${esc(pg.name)}</h3>
      <div class="meta">${esc(pg.area)}${distanceKm != null ? ` · ${distanceKm} km away` : ""}</div>
      <div class="line">
        <span class="rent">₹${pg.rent.toLocaleString("en-IN")} <small>/ month</small></span>
        <span class="stars">${pg.reviews.length ? "★ " + pg.avgRating + " (" + pg.reviews.length + ")" : "No reviews yet"}</span>
      </div>
      <div class="tags">${pg.amenities.map(a => `<span class="tag">${esc(a)}</span>`).join("")}</div>
    </article>`).join("");

  list.forEach(({ pg }) => {
    const [lng, lat] = pg.location.coordinates;
    const m = L.marker([lat, lng]).addTo(markerLayer)
      .bindPopup(`<b>${esc(pg.name)}</b><br>₹${pg.rent}/month`);
    m.on("click", () => highlight(pg.id));
    markers[pg.id] = m;
  });

  document.querySelectorAll(".pg").forEach(el => {
    el.addEventListener("click", () => {
      const m = markers[el.dataset.id];
      if (m) { map.setView(m.getLatLng(), Math.max(map.getZoom(), 15)); m.openPopup(); }
      highlight(el.dataset.id);
      openDetail(el.dataset.id);
    });
  });
}

function highlight(id) {
  document.querySelectorAll(".pg").forEach(el => el.classList.toggle("active", el.dataset.id === id));
}

/* ---------- details + reviews ---------- */
async function openDetail(id) {
  const pg = await (await fetch(`${API}/pgs/${id}`)).json();
  const reviews = pg.reviews.slice().reverse().map(r => `
    <div class="review"><span class="stars">${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</span>
    <b>${esc(r.userName)}</b><br>${esc(r.comment)}</div>`).join("") || `<p class="hint">No reviews yet. Be the first to write one.</p>`;

  $("detailBody").innerHTML = `
    <h2>${esc(pg.name)}</h2>
    <div class="meta">${esc(pg.address || "")}, ${esc(pg.area)}</div>
    <p><span class="rent">₹${pg.rent.toLocaleString("en-IN")}</span> <small>/ month</small> ·
       ${esc(pg.gender)} · sharing: ${pg.sharing.join(", ") || "n/a"} · ☎ ${esc(pg.phone || "n/a")}</p>
    <div class="tags">${pg.amenities.map(a => `<span class="tag">${esc(a)}</span>`).join("")}</div>
    <div class="actions"><button class="danger" id="btnDelete">Delete this PG</button></div>

    <h2>Reviews ${pg.reviews.length ? `(${pg.avgRating} average)` : ""}</h2>
    ${reviews}
    <h2>Write a review</h2>
    <div class="grid2">
      <div class="field"><label for="r_name">Your name</label><input id="r_name"></div>
      <div class="field"><label for="r_rating">Rating</label>
        <select id="r_rating"><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select></div>
    </div>
    <div class="field"><label for="r_comment">Comment</label><input id="r_comment"></div>
    <button class="primary" id="btnReview">Post review</button>`;

  $("btnReview").onclick = async () => {
    const body = { userName: $("r_name").value.trim() || "Anonymous", rating: +$("r_rating").value, comment: $("r_comment").value.trim() };
    await fetch(`${API}/pgs/${id}/reviews`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    await openDetail(id);
    search();
  };
  $("btnDelete").onclick = async () => {
    if (!confirm("Delete this PG permanently?")) return;
    await fetch(`${API}/pgs/${id}`, { method: "DELETE" });
    $("detail").close();
    search();
  };
  if (!$("detail").open) $("detail").showModal();
}

/* ---------- add a PG ---------- */
async function addPg(e) {
  e.preventDefault();
  const body = {
    name: $("f_name").value.trim(), gender: $("f_gender").value, rent: +$("f_rent").value,
    area: $("f_area").value.trim(), address: $("f_address").value.trim(), phone: $("f_phone").value.trim(),
    sharing: [...document.querySelectorAll("#f_sharing input:checked")].map(i => +i.value),
    amenities: [...document.querySelectorAll("#f_amenities input:checked")].map(i => i.value),
    lat: +$("f_lat").value, lng: +$("f_lng").value,
  };
  const msg = $("addMsg");
  const res = await fetch(`${API}/pgs`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
  if (res.ok) {
    msg.className = "msg ok"; msg.textContent = "PG saved. Switch to Find a PG to see it on the map.";
    $("addForm").reset();
  } else {
    msg.className = "msg err"; msg.textContent = "Could not save. Check all fields and try again.";
  }
}

/* ---------- insights ---------- */
function bars(el, rows, fmt) {
  const max = Math.max(...rows.map(r => r.v), 1);
  el.innerHTML = rows.map(r => `
    <div class="bar"><span>${esc(r.k)}</span>
    <div><div class="fill" style="width:${(r.v / max) * 100}%"></div></div>
    <span class="val">${fmt(r.v)}</span></div>`).join("");
}

async function loadInsights() {
  const [rent, top, am] = await Promise.all(
    ["avg-rent", "top-rated", "amenities"].map(p => fetch(`${API}/analytics/${p}`).then(r => r.json())));
  bars($("chartRent"), rent.map(r => ({ k: r._id, v: Math.round(r.avgRent) })), v => "₹" + v.toLocaleString("en-IN"));
  $("listTop").innerHTML = top.map(t =>
    `<li><b>${esc(t.name)}</b>, ${esc(t.area)} · ★ ${t.avgRating} (${t.reviewCount} reviews)</li>`).join("");
  bars($("chartAmenity"), am.map(a => ({ k: a._id, v: a.count })), v => v);
}

/* ---------- wire up ---------- */
function wire() {
  document.querySelectorAll(".tab").forEach(btn => btn.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach(b => b.classList.toggle("active", b === btn));
    document.querySelectorAll(".tabpane").forEach(p => p.hidden = p.id !== "tab-" + btn.dataset.tab);
    if (btn.dataset.tab === "insights") loadInsights();
  }));

  $("btnSearch").onclick = search;
  $("btnImport").onclick = importExcel;
$("btnExport").onclick = exportExcel;
  $("wholeCity").onchange = search;
  $("gender").onchange = search;
  $("radius").oninput = () => {
    $("radiusLabel").textContent = $("radius").value + " km";
    circle.setRadius(parseFloat($("radius").value) * 1000);
  };
  $("radius").onchange = search;
  $("college").onchange = () => {
    const c = COLLEGES[$("college").value];
    if (c) { setCenter(c[0], c[1]); map.setView(c, 14); }
  };
  $("btnLocate").onclick = () => {
    if (!navigator.geolocation) return alert("Your browser does not support location.");
    navigator.geolocation.getCurrentPosition(
      p => { setCenter(p.coords.latitude, p.coords.longitude); map.setView([p.coords.latitude, p.coords.longitude], 14); },
      () => alert("Could not get your location. Click the map to set it by hand."));
  };
  $("btnUseCenter").onclick = () => { $("f_lat").value = center.lat.toFixed(6); $("f_lng").value = center.lng.toFixed(6); };
  $("addForm").addEventListener("submit", addPg);
  $("btnClose").onclick = () => $("detail").close();
}

buildFilters();
initMap();
wire();
search();

/* ---------- Excel import ---------- */

async function importExcel() {
  const file = $("excelFile").files[0];
  const msg = $("importMsg");

  if (!file) {
    msg.className = "msg err";
    msg.textContent = "Please select an Excel file first.";
    return;
  }

  if (!file.name.toLowerCase().endsWith(".xlsx")) {
    msg.className = "msg err";
    msg.textContent = "Only .xlsx files are supported.";
    return;
  }

  const formData = new FormData();
  formData.append("file", file);

  try {
    $("btnImport").disabled = true;
    msg.className = "msg";
    msg.textContent = "Importing PG records...";

    const response = await fetch(`${API}/pgs/import`, {
      method: "POST",
      body: formData
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Import failed.");
    }

    msg.className = "msg ok";
    msg.textContent =
      `Imported: ${result.successfullyImported} | ` +
      `Failed: ${result.failedRecords}`;

    if (result.errors && result.errors.length > 0) {
      msg.textContent += " | Errors: " + result.errors.join("; ");
    }

    $("excelFile").value = "";

    await search();
    await loadInsights();

  } catch (error) {
    msg.className = "msg err";
    msg.textContent = error.message || "Could not import Excel file.";
  } finally {
    $("btnImport").disabled = false;
  }
}


/* ---------- Excel export ---------- */

async function exportExcel() {
  const msg = $("exportMsg");

  try {
    const params = filterParams();

    // Export nearby results unless whole-city search is selected.
    if (!$("wholeCity").checked) {
      params.set("lat", center.lat);
      params.set("lng", center.lng);
      params.set("radius", $("radius").value);
    }

    msg.className = "msg";
    msg.textContent = "Preparing Excel file...";
    $("btnExport").disabled = true;

    const response = await fetch(`${API}/pgs/export?${params}`);

    if (!response.ok) {
      throw new Error("Could not export PG listings.");
    }

    const blob = await response.blob();
    const downloadUrl = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = downloadUrl;
    link.download = "PG_Connect_Listings.xlsx";

    document.body.appendChild(link);
    link.click();
    link.remove();

   setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000); 

    msg.className = "msg ok";
    msg.textContent = "Excel file downloaded successfully.";

  } catch (error) {
    msg.className = "msg err";
    msg.textContent = error.message || "Export failed.";
  } finally {
    $("btnExport").disabled = false;
  }
}
