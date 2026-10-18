// ======================================================
//  Job Hunt Tracker: the page logic
//  This file talks to our Spring Boot API using fetch().
// ======================================================

// Short name for document.getElementById
const $ = (id) => document.getElementById(id);

// Things the page remembers while it is open
let currentUser = null;    // who is logged in: { username, role }
let currentSkill = "";     // the skill we are searching for ("" = no skill search)
let notesJobId = null;     // the job whose notes are open (null = notes panel closed)
let allJobs = [];          // the jobs we got from the server
let statusFilter = "ALL";  // ALL, NONE (not applied) or a status name
let sortBy = "newest";     // how the table is sorted

// Which status can come after which. This is the same rule as ApplicationStatus.canMoveTo
// in Java. We copy it here only to show the right choices. The SERVER still checks it.
const NEXT_STATUS = {
  SAVED: ["APPLIED"],
  APPLIED: ["INTERVIEW", "REJECTED"],
  INTERVIEW: ["OFFER", "REJECTED"],
  OFFER: [],
  REJECTED: []
};

// The four statuses we show in the summary. (A new application always starts as APPLIED.)
const SUMMARY_STATUSES = ["APPLIED", "INTERVIEW", "OFFER", "REJECTED"];

// Ways to sort. Each one is a compare function: a negative result puts a before b.
const SORTERS = {
  newest: (a, b) => b.id - a.id,
  oldest: (a, b) => a.id - b.id,
  title: (a, b) => a.title.localeCompare(b.title),
  company: (a, b) => a.company.localeCompare(b.company),
  salary: (a, b) => (b.salary ?? -1) - (a.salary ?? -1)    // ?? = "if it is null, use -1", so no salary goes last
};

// ---------- small helpers ----------

// btoa() turns text into Base64, but it only understands basic characters.
// So we first turn the text into bytes, then into a string btoa() accepts.
function toBase64(text) {
  const bytes = new TextEncoder().encode(text);
  let binary = "";
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary);
}

// The message line on the login card
function showMessage(elementId, text, isOk) {
  const el = $(elementId);
  el.textContent = text;          // textContent is safe: it never runs HTML
  el.className = isOk ? "message ok" : "message";
}

// A small message that appears in the corner and disappears by itself
function toast(text, isOk = true) {
  const box = document.createElement("div");
  box.className = isOk ? "toast" : "toast bad";
  box.textContent = text;
  $("toasts").appendChild(box);
  setTimeout(() => box.remove(), isOk ? 3500 : 6000);
}

// Our own pop-up box, used instead of the browser's prompt() and confirm().
// It returns a Promise: the text typed (or "" if there is no input) when OK is pressed,
// and null if the person cancels.
function askDialog(options) {
  return new Promise((resolve) => {
    const dlg = $("dlg");
    const field = $("dlgInput");
    $("dlgTitle").textContent = options.title;
    $("dlgText").textContent = options.text || "";
    field.hidden = !options.input;
    field.value = "";
    field.placeholder = options.input || "";
    $("dlgOk").textContent = options.okText || "OK";
    $("dlgOk").className = options.danger ? "danger" : "";
    dlg.returnValue = "";
    dlg.showModal();                  // shows the box and blocks the page behind it
    if (options.input) {
      field.focus();
    }
    // "close" runs when OK, Cancel or the Esc key closes the box
    dlg.onclose = () => resolve(dlg.returnValue === "ok" ? field.value.trim() : null);
  });
}

$("dlgCancel").addEventListener("click", () => $("dlg").close("cancel"));

// ---------- dark mode ----------

function setTheme(theme) {
  document.documentElement.dataset.theme = theme;
  $("themeBtn").textContent = theme === "dark" ? "Light mode" : "Dark mode";
  try { localStorage.setItem("theme", theme); } catch (e) { /* storage blocked: ignore */ }
}

$("themeBtn").addEventListener("click", () => {
  setTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
});
setTheme(document.documentElement.dataset.theme || "light");

// ---------- tabs ----------

function showTab(name) {
  document.querySelectorAll(".tab").forEach((button) => {
    const on = button.dataset.tab === name;
    button.classList.toggle("active", on);
    button.setAttribute("aria-selected", on);
  });
  document.querySelectorAll(".panel").forEach((panel) => {
    panel.hidden = panel.id !== "panel-" + name;
  });
}

document.querySelectorAll(".tab").forEach((button) => {
  button.addEventListener("click", () => showTab(button.dataset.tab));
});
// buttons like "Add a job" inside the empty message
document.querySelectorAll("[data-goto]").forEach((button) => {
  button.addEventListener("click", () => showTab(button.dataset.goto));
});

// ---------- talking to the server ----------

// Every request goes through this one function.
// async/await = "wait here until the server answers, without freezing the page".
async function api(path, options = {}) {
  const headers = {
    // This header tells Spring Security "this is a script, not a person at the address bar".
    // So it answers a wrong login with a plain 401, and the browser's own popup doesn't open.
    "X-Requested-With": "XMLHttpRequest"
  };

  // Our saved login (see login() below). Basic auth = "Authorization: Basic <base64 of user:password>"
  const saved = sessionStorage.getItem("auth");
  if (saved) {
    headers["Authorization"] = "Basic " + saved;
  }
  if (options.body) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(path, {
    method: options.method || "GET",
    headers: headers,
    body: options.body
  });

  // Read the answer as text first, then turn it into an object if it is JSON.
  // (A 204 answer has no body at all.)
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = null; }
  }

  if (!response.ok) {
    // 401 on a normal request = our saved login stopped working
    if (response.status === 401 && saved && !options.quiet401) {
      logout("Your login is no longer valid. Please log in again.");
    }
    // 403 = logged in, but not allowed (for example a USER trying to delete)
    if (response.status === 403) {
      const forbidden = new Error("You are not allowed to do this.");
      forbidden.status = 403;
      throw forbidden;
    }
    // Our server sends errors as {"message": "..."}, so we show that message
    const error = new Error((data && data.message) || "Request failed (" + response.status + ")");
    error.status = response.status;
    throw error;
  }
  return data;
}

// ---------- login, register, logout ----------

async function login(username, password) {
  // We keep the login in sessionStorage: it is cleared when the tab closes.
  // (localStorage would keep the password in the browser for much longer.)
  sessionStorage.setItem("auth", toBase64(username + ":" + password));
  try {
    // /api/auth/me is our "login check": it only answers if the login is correct
    const me = await api("/api/auth/me", { quiet401: true });
    showApp(me);
  } catch (e) {
    sessionStorage.removeItem("auth");
    showMessage("loginMessage",
      e.status === 401 ? "Wrong username or password." : "Could not log in: " + e.message);
  }
}

async function register(username, password) {
  try {
    await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username: username, password: password })   // object -> JSON text
    });
    await login(username, password);       // after registering, log in straight away
  } catch (e) {
    showMessage("loginMessage", e.message);
  }
}

function logout(message) {
  sessionStorage.removeItem("auth");
  currentUser = null;
  currentSkill = "";
  allJobs = [];
  $("skillSearch").value = "";
  $("appView").hidden = true;
  $("userBox").hidden = true;
  $("tabWatcher").hidden = true;
  closeNotes();
  showTab("jobs");
  $("loginView").hidden = false;
  $("password").value = "";
  showMessage("loginMessage", message || "", false);
}

function showApp(me) {
  currentUser = me;
  $("loginView").hidden = true;
  $("appView").hidden = false;
  $("userBox").hidden = false;
  $("userText").textContent = me.username;
  showMessage("loginMessage", "", false);

  // Only ADMIN sees the Watcher tab. Again this only hides it: the server blocks a USER anyway.
  const isAdmin = me.role === "ADMIN";
  $("tabWatcher").hidden = !isAdmin;
  if (isAdmin) {
    loadWatcherStatus();
  }
  showTab("jobs");
  loadJobs();
}

// ---------- the jobs list ----------

// Loads the jobs (all, or only those with the skill we are searching for).
// okMessage = a green message to show after the list is refreshed, for example "Job added."
async function loadJobs(okMessage) {
  try {
    let path = "/api/jobs";
    if (currentSkill !== "") {
      // encodeURIComponent makes the text safe to put inside a URL
      path += "?skill=" + encodeURIComponent(currentSkill);
    }
    allJobs = await api(path);
    $("listTitle").textContent = currentSkill === "" ? "My jobs" : "Jobs with skill: " + currentSkill;
    renderJobs();
    loadSummary();
    if (okMessage) {
      toast(okMessage);
    }
  } catch (e) {
    toast(e.message, false);
  }
}

// Runs one action (a function that calls the server), then refreshes the list.
// If the server refuses, we show its message instead.
async function doAction(work, okText) {
  try {
    await work();
    await loadJobs(okText);
  } catch (e) {
    toast(e.message, false);
  }
}

// Filter and sort happen here in the browser, on the jobs we already have
function visibleJobs() {
  const filtered = allJobs.filter((job) => {
    if (statusFilter === "ALL") return true;
    if (statusFilter === "NONE") return !job.status;
    return job.status === statusFilter;
  });
  return filtered.sort(SORTERS[sortBy]);    // filter() made a new array, so allJobs keeps its order
}

// Makes one table cell. The label is shown on phones (see style.css).
function makeCell(label, text) {
  const td = document.createElement("td");
  td.dataset.label = label;
  td.textContent = text;
  return td;
}

// Makes one button that runs onClick when pressed
function makeButton(text, onClick, extraClass) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "small " + (extraClass || "");
  button.textContent = text;
  button.addEventListener("click", onClick);
  return button;
}

function renderJobs() {
  const body = $("jobRows");
  body.innerHTML = "";     // empty the table first (safe: we put no data in this string)
  const jobs = visibleJobs();

  $("jobCount").textContent = jobs.length === allJobs.length
    ? "(" + jobs.length + ")"
    : "(" + jobs.length + " of " + allJobs.length + ")";

  // nothing to show: explain why and say what to do
  const empty = jobs.length === 0;
  $("emptyState").hidden = !empty;
  $("tableWrap").hidden = empty;
  if (empty) {
    $("emptyActions").hidden = allJobs.length > 0 || currentSkill !== "";
    if (currentSkill !== "") {
      $("emptyText").textContent = "No jobs have the skill " + currentSkill + ".";
    } else if (allJobs.length > 0) {
      $("emptyText").textContent = "No jobs match this filter.";
    } else {
      $("emptyText").textContent = "No jobs yet. Add one yourself or fetch some from Adzuna.";
    }
    return;
  }

  jobs.forEach((job) => {
    const tr = document.createElement("tr");

    tr.appendChild(makeCell("ID", job.id));
    tr.appendChild(makeCell("Title", job.title));
    tr.appendChild(makeCell("Company", job.company));
    tr.appendChild(makeCell("Location", job.location));

    // salary is null when Adzuna did not list one
    tr.appendChild(makeCell("Salary",
      job.salary == null ? "not listed" : "Rs. " + job.salary.toLocaleString("en-IN")));

    tr.appendChild(makeCell("Skills", job.skills.length === 0 ? "-" : job.skills.join(", ")));

    // status badge (status is null when we have not applied yet)
    const statusCell = makeCell("Status", "");
    const badge = document.createElement("span");
    badge.className = "badge " + (job.status ? job.status.toLowerCase() : "");
    badge.textContent = job.status ? job.status : "Not applied";
    statusCell.appendChild(badge);
    tr.appendChild(statusCell);

    // apply link: job data comes from the internet, so we only allow real http/https links
    const linkCell = makeCell("Link", "");
    if (job.applyLink && /^https?:\/\//.test(job.applyLink)) {
      const a = document.createElement("a");
      a.href = job.applyLink;
      a.textContent = "Open";
      a.target = "_blank";
      a.rel = "noopener noreferrer";     // stops the new page from controlling this one
      linkCell.appendChild(a);
    } else {
      linkCell.textContent = "-";
    }
    tr.appendChild(linkCell);

    tr.appendChild(makeActionsCell(job));
    body.appendChild(tr);
  });
}

// The buttons at the end of each row
function makeActionsCell(job) {
  const cell = makeCell("Actions", "");
  cell.className = "actions";

  if (!job.status) {
    // not applied yet -> an Apply button
    cell.appendChild(makeButton("Apply", () =>
      doAction(() => api("/api/jobs/" + job.id + "/apply", { method: "POST" }),
               "Marked as applied.")));
  } else {
    const choices = NEXT_STATUS[job.status] || [];
    if (choices.length === 0) {
      const done = document.createElement("span");
      done.className = "final";
      done.textContent = "Final status";
      cell.appendChild(done);
    } else {
      // a dropdown with the allowed next statuses, and an Update button
      const select = document.createElement("select");
      select.setAttribute("aria-label", "New status");
      choices.forEach((status) => {
        const option = document.createElement("option");
        option.value = status;
        option.textContent = status;
        select.appendChild(option);
      });
      cell.appendChild(select);
      cell.appendChild(makeButton("Update", () =>
        doAction(() => api("/api/jobs/" + job.id + "/status", {
                   method: "PUT",
                   body: JSON.stringify({ status: select.value })
                 }),
                 "Status changed to " + select.value + ".")));
    }
  }

  cell.appendChild(makeButton("+ Skill", () => addSkills(job)));

  // Notes belong to an application, so only jobs we applied to get a Notes button
  if (job.status) {
    cell.appendChild(makeButton("Notes", () => openNotes(job)));
  }

  // Only ADMIN sees the delete button. This only HIDES it:
  // the real protection is on the server (SecurityConfig), so a USER would still get 403.
  if (currentUser && currentUser.role === "ADMIN") {
    cell.appendChild(makeButton("Delete", async () => {
      const answer = await askDialog({
        title: "Delete this job?",
        text: "#" + job.id + " " + job.title + ". Its application and notes are deleted too.",
        okText: "Delete",
        danger: true
      });
      if (answer === null) {
        return;                // Cancel
      }
      if (notesJobId === job.id) {
        closeNotes();          // the notes are deleted with the job, so close the panel
      }
      doAction(() => api("/api/jobs/" + job.id, { method: "DELETE" }), "Job deleted.");
    }, "danger"));
  }
  return cell;
}

// Asks for skill names (in our pop-up box) and sends them one by one
async function addSkills(job) {
  const text = await askDialog({
    title: "Add skills",
    text: "For " + job.title + ". Separate skills with commas.",
    input: "Java, SQL",
    okText: "Add skills"
  });
  if (!text) {
    return;      // Cancel, or nothing typed
  }
  const names = text.split(",").map((s) => s.trim()).filter((s) => s !== "");
  if (names.length === 0) {
    return;
  }
  doAction(async () => {
    // One after the other (await inside a loop), so two new skills are never created at the same moment
    for (const name of names) {
      await api("/api/jobs/" + job.id + "/skills", {
        method: "POST",
        body: JSON.stringify({ name: name })
      });
    }
  }, "Skills added.");
}

// ---------- filter, sort, search ----------

$("filters").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-filter]");
  if (!button) {
    return;
  }
  statusFilter = button.dataset.filter;
  document.querySelectorAll("#filters button").forEach((b) => {
    b.setAttribute("aria-pressed", b === button);
  });
  renderJobs();
});

$("sortSelect").addEventListener("change", (event) => {
  sortBy = event.target.value;
  renderJobs();
});

$("searchForm").addEventListener("submit", (event) => {
  event.preventDefault();
  currentSkill = $("skillSearch").value.trim();
  loadJobs();
});

$("showAllBtn").addEventListener("click", () => {
  currentSkill = "";
  $("skillSearch").value = "";
  loadJobs();
});

$("refreshBtn").addEventListener("click", () => loadJobs());

// ---------- add a job ----------

// Full-time jobs ask for years of experience, internships ask for months
function toggleTypeFields() {
  const internship = $("jobType").value === "INTERNSHIP";
  $("yearsBox").hidden = internship;
  $("monthsBox").hidden = !internship;
}

$("jobType").addEventListener("change", toggleTypeFields);

$("addJobForm").addEventListener("submit", (event) => {
  event.preventDefault();

  const type = $("jobType").value;
  const salaryText = $("jobSalary").value.trim();

  // This object becomes the JSON body. It matches CreateJobRequest in Java.
  const job = {
    type: type,
    title: $("jobTitle").value.trim(),
    company: $("jobCompany").value.trim(),
    location: $("jobLocation").value.trim(),
    salary: salaryText === "" ? null : Number(salaryText),
    applyLink: $("jobLink").value.trim() || null
  };
  if (type === "INTERNSHIP") {
    job.durationMonths = Number($("jobMonths").value || 0);
  } else {
    job.experienceYears = Number($("jobYears").value || 0);
  }

  doAction(async () => {
    await api("/api/jobs", { method: "POST", body: JSON.stringify(job) });
    $("addJobForm").reset();     // clear the form
    toggleTypeFields();
    currentSkill = "";           // show the full list, so the new job is visible
    $("skillSearch").value = "";
    showTab("jobs");
  }, "Job added.");
});

// ---------- status summary ----------

// GET /api/applications/summary answers like {"APPLIED":2,"INTERVIEW":1}.
// A status with zero jobs is missing from the answer, so we use 0 for it.
async function loadSummary() {
  try {
    const counts = await api("/api/applications/summary");
    const boxes = $("summaryBoxes");
    const bar = $("summaryBar");
    boxes.innerHTML = "";     // empty first (safe: no data in this string)
    bar.innerHTML = "";
    let total = 0;
    SUMMARY_STATUSES.forEach((status) => { total += counts[status] || 0; });

    SUMMARY_STATUSES.forEach((status) => {
      const count = counts[status] || 0;
      const box = document.createElement("div");
      box.className = "box " + status.toLowerCase();
      const number = document.createElement("strong");
      number.textContent = count;
      const label = document.createElement("span");
      label.textContent = status.charAt(0) + status.slice(1).toLowerCase();
      box.appendChild(number);
      box.appendChild(label);
      boxes.appendChild(box);

      // the bar shows each status as a share of all applications
      if (count > 0) {
        const part = document.createElement("span");
        part.className = status.toLowerCase();
        part.style.flexGrow = count;
        bar.appendChild(part);
      }
    });
  } catch (e) {
    // the summary is only extra information, so a failure here must not break the page
  }
}

// ---------- notes ----------

// Opens the notes panel for one job and loads its notes
function openNotes(job) {
  notesJobId = job.id;
  $("notesTitle").textContent = "Notes for #" + job.id + ": " + job.title;
  $("notesPanel").hidden = false;
  $("noteText").value = "";
  loadNotes().then(() => {
    // scroll only AFTER the notes have loaded, and put the panel's top at the top of the screen
    $("notesPanel").scrollIntoView({ behavior: "smooth", block: "start" });
  });
}

function closeNotes() {
  notesJobId = null;
  $("notesPanel").hidden = true;
}

async function loadNotes() {
  try {
    const notes = await api("/api/jobs/" + notesJobId + "/notes");
    const list = $("notesList");
    list.innerHTML = "";
    if (notes.length === 0) {
      const li = document.createElement("li");
      li.className = "empty";
      li.textContent = "No notes yet.";
      list.appendChild(li);
      return;
    }
    notes.forEach((note) => {
      const li = document.createElement("li");
      const date = document.createElement("span");
      date.className = "date";
      date.textContent = note.date;
      const text = document.createElement("span");
      text.textContent = note.text;       // textContent again: notes are never run as HTML
      li.appendChild(date);
      li.appendChild(text);
      list.appendChild(li);
    });
  } catch (e) {
    toast(e.message, false);
  }
}

$("noteForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await api("/api/jobs/" + notesJobId + "/notes", {
      method: "POST",
      body: JSON.stringify({ text: $("noteText").value })
    });
    $("noteText").value = "";
    await loadNotes();
    toast("Note saved.");
  } catch (e) {
    toast(e.message, false);    // for example "You already wrote a note for this job today."
  }
});

$("notesCloseBtn").addEventListener("click", closeNotes);

// ---------- fetch jobs from Adzuna ----------

$("fetchForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const what = $("fetchWhat").value.trim();
  const where = $("fetchWhere").value.trim();
  const button = $("fetchBtn");

  button.disabled = true;                       // stop double clicks while we wait
  button.textContent = "Fetching...";
  try {
    // The words go in the URL, so encodeURIComponent turns spaces into %20 for us
    const result = await api("/api/adzuna/fetch?what=" + encodeURIComponent(what) +
                             "&where=" + encodeURIComponent(where), { method: "POST" });
    toast("Adzuna sent " + result.found + " jobs. " + result.added + " were new.");
    currentSkill = "";
    $("skillSearch").value = "";
    await loadJobs();
    if (result.added > 0) {
      showTab("jobs");
    }
  } catch (e) {
    toast(e.message, false);
  } finally {
    button.disabled = false;                    // finally runs whether it worked or failed
    button.textContent = "Fetch jobs";
  }
});

// ---------- watcher (ADMIN) ----------

// Writes the watcher state in the line under the buttons
function showWatcherStatus(status) {
  const line = $("watcherStatus");
  line.className = "status-line " + (status.running ? "on" : "off");
  line.textContent = status.running
    ? "Running: searching \"" + status.what + "\" in \"" + status.where + "\" every " + status.everyMinutes + " minutes."
    : "Not running.";
}

async function loadWatcherStatus() {
  try {
    showWatcherStatus(await api("/api/watcher/status"));
  } catch (e) {
    toast(e.message, false);
  }
}

$("watcherForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const url = "/api/watcher/start?what=" + encodeURIComponent($("watchWhat").value.trim()) +
              "&where=" + encodeURIComponent($("watchWhere").value.trim()) +
              "&minutes=" + encodeURIComponent($("watchMinutes").value);
  try {
    showWatcherStatus(await api(url, { method: "POST" }));
    toast("Watcher started.");
  } catch (e) {
    toast(e.message, false);
  }
});

$("watchStopBtn").addEventListener("click", async () => {
  try {
    showWatcherStatus(await api("/api/watcher/stop", { method: "POST" }));
    toast("Watcher stopped.");
  } catch (e) {
    toast(e.message, false);
  }
});

$("watchStatusBtn").addEventListener("click", () => loadWatcherStatus());

// ---------- login form and top buttons ----------

$("loginForm").addEventListener("submit", (event) => {
  event.preventDefault();      // stop the browser from reloading the page
  login($("username").value.trim(), $("password").value);
});

$("registerBtn").addEventListener("click", () => {
  register($("username").value.trim(), $("password").value);
});

$("logoutBtn").addEventListener("click", () => logout(""));

// ---------- when the page opens ----------

// If a login is still saved from earlier in this tab, check it and go straight to the app
async function start() {
  if (!sessionStorage.getItem("auth")) {
    return;
  }
  try {
    const me = await api("/api/auth/me", { quiet401: true });
    showApp(me);
  } catch (e) {
    logout("");
  }
}

start();
