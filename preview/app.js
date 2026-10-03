(() => {
  'use strict';
  // Appearance overrides are preview-only and are never saved as app preferences.
  const previewTheme = new URLSearchParams(location.search).get('theme');
  if (previewTheme === 'light' || previewTheme === 'dark') document.documentElement.dataset.previewTheme = previewTheme;
  const $ = id => document.getElementById(id);
  const icons = {
    drop: '<path d="M12 3C10 6 5 10 5 14C5 23 19 23 19 14C19 10 14 6 12 3Z" fill="#b8d6e6"/>',
    glass: '<path d="M6 3h12l-2 18H8Z" fill="#b8d6e6"/><path d="M7 11q3-3 5 0q3 2 5-1"/>',
    settings: '<path d="M3 6h18M3 12h18M3 18h18"/><circle cx="9" cy="6" r="2.4" fill="#faf8f3"/><circle cx="16" cy="12" r="2.4" fill="#faf8f3"/><circle cx="7" cy="18" r="2.4" fill="#faf8f3"/>',
    back: '<path d="m14 5-7 7 7 7M7 12h14"/>',
    clock: '<circle cx="12" cy="12" r="8.5" fill="#d7cced"/><path d="M12 7v5l3.5 2"/>',
    moon: '<path d="M15 3C2 0 0 20 13 21q6 1 9-6C12 18 9 8 15 3Z" fill="#d7cced"/>',
    sun: '<circle cx="12" cy="12" r="5" fill="#eabda6"/><path d="M12 1v2m0 18v2M1 12h2m18 0h2M4 4l1.5 1.5M18.5 18.5 20 20M4 20l1.5-1.5M18.5 5.5 20 4"/>',
    bell: '<path d="m4 17 2-3V9C6 1 18 1 18 9v5l2 3ZM9 21q3 2 6 0" fill="#eabda6"/>'
  };
  document.querySelectorAll('[data-icon]').forEach(el => {
    el.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="#4b4858" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[el.dataset.icon]}</svg>`;
  });
  const defaults = { enabled:true, intervalMinutes:120, quietEnabled:true, quietStart:1320, quietEnd:420, wakeScreen:true, nextAt:0 };
  let saved;
  try { saved = JSON.parse(localStorage.getItem('waterminder-preview')); } catch (_) {}
  let state = {...defaults, ...saved};
  if (!Number.isInteger(state.intervalMinutes) || state.intervalMinutes < 30 || state.intervalMinutes > 480) state.intervalMinutes = 120;
  if (!Number.isInteger(state.quietStart) || !Number.isInteger(state.quietEnd) || state.quietStart < 0 || state.quietStart > 1439 || state.quietEnd < 0 || state.quietEnd > 1439 || state.quietStart === state.quietEnd) { state.quietStart = 1320; state.quietEnd = 420; }
  let happyTimer, toastTimer, previousFocus;
  const time = minutes => new Date(2026,0,1,Math.floor(minutes/60),minutes%60).toLocaleTimeString([], {hour:'numeric', minute:minutes%60 ? '2-digit' : undefined});
  const inputTime = minutes => `${String(Math.floor(minutes/60)).padStart(2,'0')}:${String(minutes%60).padStart(2,'0')}`;
  const interval = minutes => minutes < 60 ? `${minutes} min` : minutes%60 ? `${Math.floor(minutes/60)} hr ${minutes%60} min` : `${minutes/60} ${minutes===60?'hour':'hours'}`;
  const minuteOfDay = date => date.getHours()*60+date.getMinutes();
  function isQuiet(date) {
    if (!state.quietEnabled) return false;
    const minutes=minuteOfDay(date);
    return state.quietStart < state.quietEnd ? minutes>=state.quietStart && minutes<state.quietEnd : minutes>=state.quietStart || minutes<state.quietEnd;
  }
  function nextAllowed(date) {
    if (!isQuiet(date)) return date;
    const result = new Date(date);
    if (state.quietStart>state.quietEnd && minuteOfDay(date)>=state.quietStart) result.setDate(result.getDate()+1);
    result.setHours(Math.floor(state.quietEnd/60),state.quietEnd%60,0,0);
    return result;
  }
  function save() { try { localStorage.setItem('waterminder-preview',JSON.stringify(state)); } catch (_) {} }
  function schedule() { state.nextAt=state.enabled?nextAllowed(new Date(Date.now()+state.intervalMinutes*60000)).getTime():0; save(); }
  function paint() {
    $('enabled-toggle').setAttribute('aria-checked',String(state.enabled));
    $('quiet-toggle').setAttribute('aria-checked',String(state.quietEnabled));
    $('wake-toggle').setAttribute('aria-checked',String(state.wakeScreen));
    $('quiet-times').hidden = !state.quietEnabled;
    $('quiet-start').value=inputTime(state.quietStart); $('quiet-end').value=inputTime(state.quietEnd);
    document.querySelectorAll('[data-minutes]').forEach(button=>button.setAttribute('aria-pressed',String(Number(button.dataset.minutes)===state.intervalMinutes)));
    $('custom-button').textContent=[60,120,180,240].includes(state.intervalMinutes)?'Or choose your own interval':`Custom interval: ${interval(state.intervalMinutes)}`;
    $('schedule-summary').textContent=`every ${interval(state.intervalMinutes)} · ${state.quietEnabled?`quiet ${time(state.quietStart)}–${time(state.quietEnd)}`:'quiet hours off'}`;
    $('next-label').textContent=state.enabled?(isQuiet(new Date())?'Resting during quiet hours':'Next little nudge'):'Reminders are off';
    if (!state.enabled) $('next-time').textContent='Start your rhythm';
    else { const next=new Date(state.nextAt); $('next-time').textContent=(next.toDateString()!==new Date().toDateString()?'tomorrow, ':'around ')+next.toLocaleTimeString([],{hour:'numeric',minute:'2-digit'}); }
  }
  function showSettings(open) {
    if (open) resetPlant();
    $('home').hidden=open; $('settings').hidden=!open;
    document.querySelector('.brand').hidden=open; $('settings-button').hidden=open; $('back-button').hidden=!open; document.querySelector('.settings-title').hidden=!open;
    (open?$('back-button'):$('settings-button')).focus();
  }
  function drink() {
    schedule(); paint(); clearTimeout(happyTimer);
    $('plant').classList.remove('watering'); void $('plant').offsetWidth; $('plant').classList.add('watering');
    $('plant').setAttribute('aria-label','A happy little plant being watered');
    $('greeting').innerHTML='a sip for you.<br>a splash for your plant.';
    $('subtitle').textContent='Look at you, taking care of yourself.';
    $('plant-note').textContent='a little happier already'; $('drink-label').textContent='You & your plant say thanks';
    happyTimer=setTimeout(resetPlant,4600);
  }
  function resetPlant() {
    clearTimeout(happyTimer);
    $('plant').classList.remove('watering');
    $('plant').setAttribute('aria-label','A smiling little plant in a peach pot');
    $('greeting').innerHTML='a little water.<br>a little better.';
    $('subtitle').textContent='Small sips. A softer kind of habit.';
    $('plant-note').textContent='you grow at your own pace'; $('drink-label').textContent='I drank water';
  }
  function toast(text) { clearTimeout(toastTimer); $('toast').textContent=text; $('toast').hidden=false; toastTimer=setTimeout(()=>{$('toast').hidden=true;},4000); }
  function openDialog(id) { previousFocus=document.activeElement; $(id).hidden=false; $(id).querySelector('button,input').focus(); }
  function closeDialog(id) { $(id).hidden=true; if(previousFocus) previousFocus.focus(); }
  $('settings-button').onclick=()=>showSettings(true); $('back-button').onclick=()=>showSettings(false);
  $('drink').onclick=drink;
  $('enabled-toggle').onclick=()=>{ state.enabled=!state.enabled; schedule(); paint(); };
  $('quiet-toggle').onclick=()=>{ state.quietEnabled=!state.quietEnabled; schedule(); paint(); };
  $('wake-toggle').onclick=()=>{ state.wakeScreen=!state.wakeScreen; save(); paint(); };
  document.querySelectorAll('[data-minutes]').forEach(button=>button.onclick=()=>{state.intervalMinutes=Number(button.dataset.minutes);schedule();paint();});
  for (const [id,key] of [['quiet-start','quietStart'],['quiet-end','quietEnd']]) $(id).onchange=()=>{
    if(!$(id).value) {paint();return;}
    const [h,m]=$(id).value.split(':').map(Number),value=h*60+m,other=key==='quietStart'?'quietEnd':'quietStart';
    if(value===state[other]) { $('quiet-error').hidden=false; paint(); return; }
    state[key]=value; $('quiet-error').hidden=true; schedule(); paint();
  };
  $('custom-button').onclick=()=>{ $('interval-range').value=state.intervalMinutes; $('custom-value').textContent=`Every ${interval(state.intervalMinutes)}`; openDialog('custom-overlay'); };
  $('interval-range').oninput=()=>{$('custom-value').textContent=`Every ${interval(Number($('interval-range').value))}`;};
  $('custom-form').onsubmit=event=>{event.preventDefault();state.intervalMinutes=Number($('interval-range').value);schedule();paint();closeDialog('custom-overlay');};
  $('cancel-custom').onclick=()=>closeDialog('custom-overlay');
  $('test-nudge').onclick=()=>{if(isQuiet(new Date()))toast("Shh, it's quiet hours. Try again after they end.");else openDialog('notification-overlay');};
  $('close-notification').onclick=()=>closeDialog('notification-overlay');
  $('notification-drink').onclick=()=>{closeDialog('notification-overlay');showSettings(false);drink();};
  document.addEventListener('keydown',event=>{
    const open=['custom-overlay','notification-overlay'].find(id=>!$(id).hidden);
    if(!open) return;
    if(event.key==='Escape') closeDialog(open);
    if(event.key==='Tab') { const controls=[...$(open).querySelectorAll('button,input')]; const first=controls[0],last=controls[controls.length-1]; if(event.shiftKey && document.activeElement===first){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();} }
  });
  if(!state.nextAt || state.nextAt<Date.now())schedule();paint();
  setInterval(()=>{if(state.enabled&&state.nextAt<Date.now())schedule();paint();},30000);
})();
