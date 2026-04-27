const state = {
  user: null,
  contacts: [],
  filtered: [],
  page: 1,
  pageSize: 10,
  editingContactId: null
};

const API_BASE = "/api";

const loginCard = document.getElementById("loginCard");
const loginForm = document.getElementById("loginForm");
const loginBtn = document.getElementById("loginBtn");
const loginError = document.getElementById("loginError");
const loginOpenBtn = document.getElementById("loginOpenBtn");
const closeLoginBtn = document.getElementById("closeLoginBtn");
const logoutBtn = document.getElementById("logoutBtn");

const appError = document.getElementById("appError");
const searchInput = document.getElementById("searchInput");
const pageSizeSelect = document.getElementById("pageSizeSelect");
const contactsBody = document.getElementById("contactsBody");
const counter = document.getElementById("counter");
const pageInfo = document.getElementById("pageInfo");
const prevBtn = document.getElementById("prevBtn");
const nextBtn = document.getElementById("nextBtn");
const welcome = document.getElementById("welcome");
const actionsHeader = document.getElementById("actionsHeader");

const editPanel = document.getElementById("editPanel");
const editTitle = document.getElementById("editTitle");
const contactForm = document.getElementById("contactForm");
const contactId = document.getElementById("contactId");
const setorInput = document.getElementById("setorInput");
const telefoneInput = document.getElementById("telefoneInput");
const localInput = document.getElementById("localInput");
const cancelEditBtn = document.getElementById("cancelEditBtn");

loginForm.addEventListener("submit", onLogin);
loginOpenBtn.addEventListener("click", () => toggleLoginCard(true));
closeLoginBtn.addEventListener("click", () => toggleLoginCard(false));
logoutBtn.addEventListener("click", onLogout);
searchInput.addEventListener("input", onSearch);
pageSizeSelect.addEventListener("change", onPageSizeChange);
prevBtn.addEventListener("click", () => changePage(-1));
nextBtn.addEventListener("click", () => changePage(1));
contactForm.addEventListener("submit", onSaveContact);
cancelEditBtn.addEventListener("click", resetEditForm);

bootstrap();

async function bootstrap() {
  const cachedUser = localStorage.getItem("agendaUser");
  if (cachedUser) {
    try {
      state.user = JSON.parse(cachedUser);
    } catch (_) {
      localStorage.removeItem("agendaUser");
    }
  }

  updateAuthUi();
  await loadContacts();
}

function updateAuthUi() {
  const isAuthenticated = Boolean(state.user);
  loginOpenBtn.classList.toggle("hidden", isAuthenticated);
  logoutBtn.classList.toggle("hidden", !isAuthenticated);
  editPanel.classList.toggle("hidden", !isAuthenticated);
  actionsHeader.classList.toggle("hidden", !isAuthenticated);

  if (isAuthenticated) {
    welcome.textContent = `Conectado como ${state.user.username}. Edicao habilitada.`;
  } else {
    welcome.textContent = "Agenda publica. Clique em Entrar para editar contatos.";
    resetEditForm();
  }

  // Defensive cleanup in case stale action cells remain visible after auth state changes.
  syncActionsColumnVisibility(isAuthenticated);
}

function toggleLoginCard(visible) {
  loginCard.classList.toggle("hidden", !visible);
  document.body.classList.toggle("login-open", visible);
  if (!visible) {
    hideLoginError();
    loginForm.reset();
  }
}

async function onLogin(event) {
  event.preventDefault();
  hideLoginError();

  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;

  loginBtn.disabled = true;

  try {
    const response = await fetch(`${API_BASE}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const payload = await tryParseJson(response);

    if (!response.ok) {
      const message = payload?.message || "Falha no login.";
      throw new Error(message);
    }

    state.user = payload;
    localStorage.setItem("agendaUser", JSON.stringify(payload));
    updateAuthUi();
    toggleLoginCard(false);
    await loadContacts();
  } catch (error) {
    showLoginError(error.message || "Erro inesperado ao autenticar.");
  } finally {
    loginBtn.disabled = false;
  }
}

function onLogout() {
  state.user = null;
  localStorage.removeItem("agendaUser");
  toggleLoginCard(false);
  updateAuthUi();
  renderTable();
}

function syncActionsColumnVisibility(isAuthenticated) {
  if (isAuthenticated) {
    return;
  }

  if (actionsHeader) {
    actionsHeader.classList.add("hidden");
  }

  contactsBody.querySelectorAll("tr").forEach((row) => {
    if (row.children.length > 3) {
      row.removeChild(row.lastElementChild);
    }
  });
}

async function loadContacts() {
  hideAppError();

  try {
    const response = await fetch(`${API_BASE}/contatos`);
    if (!response.ok) {
      throw new Error("Nao foi possivel carregar contatos.");
    }

    state.contacts = await response.json();
    state.filtered = [...state.contacts];
    state.page = 1;
    renderTable();
  } catch (error) {
    showAppError(error.message || "Erro ao carregar contatos.");
  }
}

function onSearch() {
  const value = searchInput.value.trim().toLowerCase();

  if (!value) {
    state.filtered = [...state.contacts];
  } else {
    state.filtered = state.contacts.filter((contact) => {
      return [contact.setor, contact.telefone, contact.local]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(value));
    });
  }

  state.page = 1;
  renderTable();
}

function onPageSizeChange() {
  state.pageSize = Number(pageSizeSelect.value);
  state.page = 1;
  renderTable();
}

function changePage(direction) {
  const totalPages = getTotalPages();
  const nextPage = state.page + direction;

  if (nextPage < 1 || nextPage > totalPages) {
    return;
  }

  state.page = nextPage;
  renderTable();
}

function renderTable() {
  const total = state.filtered.length;
  const totalPages = getTotalPages();
  const page = Math.min(state.page, totalPages);
  state.page = page;

  const start = (page - 1) * state.pageSize;
  const end = Math.min(start + state.pageSize, total);
  const slice = state.filtered.slice(start, end);

  contactsBody.innerHTML = "";
  const isAuthenticated = Boolean(state.user);

  if (slice.length === 0) {
    const row = document.createElement("tr");
    row.innerHTML = `<td colspan='${isAuthenticated ? 4 : 3}'>Nenhum contato encontrado.</td>`;
    contactsBody.appendChild(row);
  } else {
    slice.forEach((contact) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${escapeHtml(contact.setor || "")}</td>
        <td>${escapeHtml(contact.telefone || "")}</td>
        <td>${escapeHtml(contact.local || "")}</td>
      `;

      if (isAuthenticated) {
        const actionsCell = document.createElement("td");
        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.className = "ghost";
        editButton.textContent = "Editar";
        editButton.addEventListener("click", () => beginEdit(contact));

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "ghost";
        deleteButton.textContent = "Excluir";
        deleteButton.addEventListener("click", () => removeContact(contact));

        actionsCell.appendChild(editButton);
        actionsCell.appendChild(document.createTextNode(" "));
        actionsCell.appendChild(deleteButton);
        row.appendChild(actionsCell);
      }

      contactsBody.appendChild(row);
    });
  }

  const totalSafe = total === 0 ? 0 : total;
  const startLabel = total === 0 ? 0 : start + 1;
  counter.textContent = `Exibindo ${startLabel} a ${end} de ${totalSafe} registros`;
  pageInfo.textContent = `Pagina ${page} de ${totalPages}`;

  prevBtn.disabled = page <= 1;
  nextBtn.disabled = page >= totalPages;
}

function getTotalPages() {
  return Math.max(1, Math.ceil(state.filtered.length / state.pageSize));
}

function beginEdit(contact) {
  state.editingContactId = contact.id;
  contactId.value = String(contact.id);
  setorInput.value = contact.setor || "";
  telefoneInput.value = contact.telefone || "";
  localInput.value = contact.local || "";
  editTitle.textContent = `Editar contato #${contact.id}`;
}

function resetEditForm() {
  state.editingContactId = null;
  contactForm.reset();
  contactId.value = "";
  editTitle.textContent = "Novo contato";
}

async function onSaveContact(event) {
  event.preventDefault();
  if (!state.user) {
    return;
  }

  hideAppError();

  const payload = {
    setor: setorInput.value.trim(),
    telefone: telefoneInput.value.trim(),
    local: localInput.value.trim()
  };

  const isEditing = Boolean(state.editingContactId);
  const path = isEditing
    ? `${API_BASE}/contatos/${state.editingContactId}`
    : `${API_BASE}/contatos`;
  const method = isEditing ? "PUT" : "POST";

  try {
    const response = await fetch(path, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const data = await tryParseJson(response);
    if (!response.ok) {
      throw new Error(data?.message || "Nao foi possivel salvar contato.");
    }

    resetEditForm();
    await loadContacts();
  } catch (error) {
    showAppError(error.message || "Erro ao salvar contato.");
  }
}

async function removeContact(contact) {
  if (!state.user) {
    return;
  }

  const confirmDelete = window.confirm(`Excluir contato ${contact.setor}?`);
  if (!confirmDelete) {
    return;
  }

  hideAppError();

  try {
    const response = await fetch(`${API_BASE}/contatos/${contact.id}`, {
      method: "DELETE"
    });

    if (!response.ok) {
      const data = await tryParseJson(response);
      throw new Error(data?.message || "Nao foi possivel excluir contato.");
    }

    await loadContacts();
  } catch (error) {
    showAppError(error.message || "Erro ao excluir contato.");
  }
}

function showLoginError(message) {
  loginError.textContent = message;
  loginError.classList.remove("hidden");
}

function hideLoginError() {
  loginError.textContent = "";
  loginError.classList.add("hidden");
}

function showAppError(message) {
  appError.textContent = message;
  appError.classList.remove("hidden");
}

function hideAppError() {
  appError.textContent = "";
  appError.classList.add("hidden");
}

async function tryParseJson(response) {
  try {
    return await response.json();
  } catch (_) {
    return null;
  }
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
