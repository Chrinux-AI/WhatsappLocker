const key = 'wa_locker_pin';

document.getElementById('savePin').onclick = () => {
  const pin = document.getElementById('pin').value.trim();
  if (pin.length < 4) return alert('PIN must be at least 4 digits.');
  localStorage.setItem(key, btoa(pin));
  alert('PIN saved in browser storage.');
};

document.getElementById('unlock').onclick = () => {
  const pin = document.getElementById('pin').value.trim();
  if (!localStorage.getItem(key)) return alert('Set a PIN first.');
  if (btoa(pin) === localStorage.getItem(key)) {
    location.href = 'https://web.whatsapp.com';
  } else {
    alert('Incorrect PIN');
  }
};
