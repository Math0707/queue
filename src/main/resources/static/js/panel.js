const PRIORITY_LABEL = { LOW: "Baixa", MEDIUM: "Média", HIGH: "Alta" };
const STATUS_LABEL = {
  PENDING: "Aguardando",
  PROCESSING: "Em atendimento",
  DONE: "Concluído",
  ERROR: "Erro",
};

function priorityClass(p) {
  if (p === "HIGH") return "chip--alta";
  if (p === "MEDIUM") return "chip--media";
  return "chip--baixa";
}

function statusClass(s) {
  if (s === "PENDING") return "chip--aguardando";
  if (s === "PROCESSING") return "chip--atendendo";
  if (s === "DONE") return "chip--ok";
  return "";
}

function formatDateTime(isoOrLocal) {
  if (!isoOrLocal) return "—";
  try {
    const d = new Date(isoOrLocal);
    if (Number.isNaN(d.getTime())) return isoOrLocal;
    return d.toLocaleString("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    });
  } catch {
    return isoOrLocal;
  }
}

function showFlash(el, type, msg) {
  if (!el) return;
  el.className = `flash flash--${type}`;
  el.textContent = msg;
  el.hidden = false;
}

function hideFlash(el) {
  if (!el) return;
  el.hidden = true;
  el.textContent = "";
}

async function fetchJson(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    ...options,
  });
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  if (!res.ok) {
    const err = new Error(res.statusText || "Erro na requisição");
    err.status = res.status;
    err.body = data;
    throw err;
  }
  return data;
}

function renderQueueList(container, tasks) {
  if (!tasks || tasks.length === 0) {
    container.innerHTML =
      '<p class="empty-state">Ninguém na fila para chamar agora.<br>Pode ser que todos já estejam em atendimento ou ainda estejam agendados para depois.</p>';
    return;
  }
  const items = tasks
    .map((t) => {
      const plab = PRIORITY_LABEL[t.priority] || t.priority;
      const pcl = priorityClass(t.priority);
      return `<li class="queue-item">
        <div class="queue-item__top">
          <span class="queue-item__id">Chamado #${t.id}</span>
          <span class="chip ${pcl}">${plab}</span>
        </div>
        <div class="queue-item__desc">${escapeHtml(t.description)}</div>
        <div class="queue-item__meta">Entrou: ${formatDateTime(t.createdAt)}${
        t.scheduledFor
          ? ` · Liberado a partir de: ${formatDateTime(t.scheduledFor)}`
          : ""
      }</div>
      </li>`;
    })
    .join("");
  container.innerHTML = `<ul class="queue-list">${items}</ul>`;
}

function escapeHtml(s) {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

function renderAllTasks(container, tasks) {
  if (!tasks || tasks.length === 0) {
    container.innerHTML = '<p class="empty-state">Nenhuma solicitação cadastrada ainda.</p>';
    return;
  }
  const sorted = [...tasks].sort((a, b) => (b.id || 0) - (a.id || 0));
  const items = sorted
    .map((t) => {
      const plab = PRIORITY_LABEL[t.priority] || t.priority;
      const slab = STATUS_LABEL[t.status] || t.status;
      const pcl = priorityClass(t.priority);
      const scl = statusClass(t.status);
      return `<li class="queue-item">
        <div class="queue-item__top">
          <span class="queue-item__id">#${t.id}</span>
          <span style="display:flex;gap:0.35rem;flex-wrap:wrap;">
            <span class="chip ${pcl}">${plab}</span>
            <span class="chip ${scl}">${slab}</span>
          </span>
        </div>
        <div class="queue-item__desc">${escapeHtml(t.description)}</div>
        <div class="queue-item__meta">Criado: ${formatDateTime(t.createdAt)}${
        t.scheduledFor ? ` · Agendado: ${formatDateTime(t.scheduledFor)}` : ""
      }${
        t.processedAt && t.status === "DONE"
          ? ` · Encerrado: ${formatDateTime(t.processedAt)}`
          : ""
      }</div>
      </li>`;
    })
    .join("");
  container.innerHTML = `<ul class="queue-list">${items}</ul>`;
}

function init() {
  const formFlash = document.getElementById("form-flash");
  const queueFlash = document.getElementById("queue-flash");
  const actionFlash = document.getElementById("action-flash");
  const queueList = document.getElementById("queue-list");
  const allList = document.getElementById("all-tasks");
  const form = document.getElementById("new-task-form");

  async function refreshQueue() {
    hideFlash(queueFlash);
    try {
      const data = await fetchJson("/queue");
      renderQueueList(queueList, data);
    } catch (e) {
      showFlash(queueFlash, "err", "Não foi possível carregar a fila. Verifique se a API está rodando.");
    }
  }

  async function refreshAll() {
    try {
      const data = await fetchJson("/tasks");
      renderAllTasks(allList, data);
    } catch {
      allList.innerHTML = '<p class="empty-state">Não foi possível carregar a lista completa.</p>';
    }
  }

  document.getElementById("btn-refresh-queue").addEventListener("click", () => {
    refreshQueue();
    refreshAll();
  });

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    hideFlash(formFlash);
    const desc = document.getElementById("field-description").value.trim();
    const priority = document.getElementById("field-priority").value;
    const dt = document.getElementById("field-scheduled").value;

    const body = { description: desc, priority };
    if (dt) {
      body.scheduledFor = dt.length === 16 ? `${dt}:00` : dt;
    }

    try {
      await fetchJson("/tasks", { method: "POST", body: JSON.stringify(body) });
      form.reset();
      showFlash(formFlash, "ok", "Incluído na fila com sucesso.");
      refreshQueue();
      refreshAll();
    } catch (e) {
      let msg = "Não foi possível incluir. ";
      if (e.body && e.body.errors) {
        msg += Object.values(e.body.errors).join(" ");
      } else if (typeof e.body === "string") {
        msg += e.body;
      } else if (e.body && e.body.title) {
        msg += e.body.title;
      } else {
        msg += "Confira os dados e tente de novo.";
      }
      showFlash(formFlash, "err", msg);
    }
  });

  document.getElementById("btn-claim").addEventListener("click", async () => {
    hideFlash(actionFlash);
    try {
      const res = await fetch("/queue/claim", { method: "POST" });
      if (res.status === 404) {
        showFlash(actionFlash, "err", "Não há ninguém na fila para chamar agora.");
        return;
      }
      if (!res.ok) throw new Error();
      const t = await res.json();
      const plab = PRIORITY_LABEL[t.priority] || t.priority;
      showFlash(
        actionFlash,
        "ok",
        `Chamando chamado #${t.id} — ${t.description} (${plab}). Encerre quando terminar, abaixo.`
      );
      document.getElementById("field-complete-id").value = t.id;
      refreshQueue();
      refreshAll();
    } catch {
      showFlash(actionFlash, "err", "Erro ao chamar o próximo.");
    }
  });

  document.getElementById("btn-complete").addEventListener("click", async () => {
    hideFlash(actionFlash);
    const idRaw = document.getElementById("field-complete-id").value.trim();
    const id = parseInt(idRaw, 10);
    if (!id) {
      showFlash(actionFlash, "err", "Informe o número do chamado ou chame o próximo antes.");
      return;
    }
    try {
      await fetchJson(`/tasks/${id}/complete`, { method: "POST" });
      showFlash(actionFlash, "ok", `Chamado #${id} marcado como concluído.`);
      document.getElementById("field-complete-id").value = "";
      refreshQueue();
      refreshAll();
    } catch (e) {
      const detail =
        e.body && e.body.detail
          ? e.body.detail
          : e.body && e.body.message
            ? e.body.message
            : "Só é possível concluir quem já está em atendimento.";
      showFlash(actionFlash, "err", String(detail));
    }
  });

  refreshQueue();
  refreshAll();
}

document.addEventListener("DOMContentLoaded", init);
