const modal = document.querySelector('#create-modal');
const sessionModal = document.querySelector('#session-modal');
const sessionFrame = document.querySelector('#vnc-frame');
const screenLoading = document.querySelector('#screen-loading');
const openCreate = document.querySelector('#open-create');
const closeModal = document.querySelector('#close-modal');
const createMachine = document.querySelector('#create-machine');
const toast = document.querySelector('#toast');
const machineInput = document.querySelector('#machine-name-input');

function setModal(open) {
  modal.classList.toggle('open', open);
  modal.setAttribute('aria-hidden', String(!open));
  if (open) machineInput.focus();
}

openCreate.addEventListener('click', () => setModal(true));
closeModal.addEventListener('click', () => setModal(false));
modal.addEventListener('click', (event) => {
  if (event.target === modal) setModal(false);
});

document.querySelectorAll('.browser-option').forEach((option) => {
  option.addEventListener('click', () => {
    document.querySelectorAll('.browser-option').forEach((item) => item.classList.remove('selected'));
    option.classList.add('selected');
  });
});

document.querySelectorAll('.launch-button').forEach((button) => {
  button.addEventListener('click', () => {
    const machine = button.dataset.machine;
    const browser = machine.toLowerCase().includes('firefox') ? 'firefox' : machine.toLowerCase().includes('brave') ? 'brave' : 'chrome';
    openSession(browser, machine);
  });
});

createMachine.addEventListener('click', () => {
  const browser = document.querySelector('.browser-option.selected strong').textContent;
  const name = machineInput.value.trim() || 'My browser session';
  setModal(false);
  openSession(browser.toLowerCase(), name, true);
});

async function openSession(browser, name, deploying = false) {
  sessionModal.classList.add('open');
  sessionModal.setAttribute('aria-hidden', 'false');
  screenLoading.classList.remove('hidden');
  document.querySelector('#session-title').textContent = name;
  try {
    const response = await fetch('/api/machines', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ browser, name }) });
    if (!response.ok) throw new Error('Container unavailable');
    const machine = await response.json();
    sessionFrame.src = machine.vncUrl;
    sessionFrame.onload = () => screenLoading.classList.add('hidden');
    if (deploying) showToast('Machine deployed', `${machine.browser} is running in a Linux container.`);
  } catch (error) {
    screenLoading.querySelector('strong').textContent = 'Container unavailable';
    screenLoading.querySelector('small').textContent = 'Start the BrowserRTP server and try again.';
    showToast('Connection failed', error.message);
  }
}

function closeSession() {
  sessionModal.classList.remove('open');
  sessionModal.setAttribute('aria-hidden', 'true');
  sessionFrame.src = 'about:blank';
}

document.querySelector('#close-session').addEventListener('click', closeSession);
document.querySelector('#stop-session').addEventListener('click', () => { closeSession(); showToast('Session stopped', 'The remote view has been closed.'); });
document.querySelector('#keyboard-toggle').addEventListener('click', () => document.querySelector('#virtual-keyboard').classList.toggle('open'));
document.querySelectorAll('.virtual-keyboard button').forEach((key) => key.addEventListener('click', () => key.classList.add('pressed')));

function showToast(title, message) {
  toast.querySelector('strong').textContent = title;
  toast.querySelector('small').textContent = message;
  toast.classList.add('show');
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove('show'), 3600);
}

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') setModal(false);
});
