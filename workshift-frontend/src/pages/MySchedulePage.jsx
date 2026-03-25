import { useEffect, useState, useMemo } from 'react'
import { useParams, useOutletContext } from 'react-router-dom'
import { getMyCalendar } from '../features/calendar/calendarApi'
import { registerShift, cancelRegistration } from '../features/registrations/registrationApi'
import { getShifts } from '../features/shifts/shiftApi'
import { getPositions } from '../features/positions/positionApi'

/* ───── helpers ───── */
function startOfWeek(d) {
  const dt = new Date(d); const day = dt.getDay()
  const diff = day === 0 ? -6 : 1 - day
  dt.setDate(dt.getDate() + diff); dt.setHours(0, 0, 0, 0)
  return dt
}
function addDays(d, n) { const r = new Date(d); r.setDate(r.getDate() + n); return r }
function fmtISO(d) { return d.toISOString().slice(0, 10) }
function fmtTime(t) { return t ? String(t).substring(0, 5) : '—' }

const DAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'CN']
const MONTH_NAMES = ['Th01', 'Th02', 'Th03', 'Th04', 'Th05', 'Th06', 'Th07', 'Th08', 'Th09', 'Th10', 'Th11', 'Th12']

const REG_STATUS_CFG = {
  PENDING:  { label: 'Chờ duyệt', cls: 'bg-amber-100 text-amber-700', icon: 'hourglass_top' },
  APPROVED: { label: 'Đã duyệt', cls: 'bg-emerald-100 text-emerald-700', icon: 'check_circle' },
  REJECTED: { label: 'Từ chối', cls: 'bg-red-100 text-red-700', icon: 'cancel' },
  CANCELLED:{ label: 'Đã hủy', cls: 'bg-slate-100 text-slate-600', icon: 'block' },
}

export function MySchedulePage() {
  const { groupId } = useParams()
  const { isManager } = useOutletContext() || {}

  // Tab: 'calendar' or 'available'
  const [tab, setTab] = useState('calendar')
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()))

  // Calendar data (B19)
  const [calendarItems, setCalendarItems] = useState([])
  const [loadingCal, setLoadingCal] = useState(true)

  // Available shifts (B11)
  const [shifts, setShifts] = useState([])
  const [positions, setPositions] = useState([])
  const [loadingShifts, setLoadingShifts] = useState(false)

  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)

  // Register modal (B12)
  const [regShift, setRegShift] = useState(null)
  const [regPos, setRegPos] = useState('')
  const [regNote, setRegNote] = useState('')
  const [registering, setRegistering] = useState(false)

  // Cancel modal (B13)
  const [cancelItem, setCancelItem] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)

  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart])
  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart])

  /* ───── Load my calendar (B19) ───── */
  async function loadCalendar() {
    setLoadingCal(true); setError(null)
    try {
      const res = await getMyCalendar({ from: fmtISO(weekStart), to: fmtISO(weekEnd) })
      const list = Array.isArray(res) ? res : (res?.data ?? [])
      setCalendarItems(list)
    } catch (err) {
      setError(err?.message || 'Không thể tải lịch cá nhân')
    } finally { setLoadingCal(false) }
  }

  /* ───── Load available shifts (B11) ───── */
  async function loadShifts() {
    setLoadingShifts(true); setError(null)
    try {
      const [sRes, pRes] = await Promise.all([
        getShifts(groupId, fmtISO(weekStart), fmtISO(weekEnd)),
        getPositions(groupId),
      ])
      setShifts(Array.isArray(sRes) ? sRes : (sRes?.data ?? []))
      setPositions(Array.isArray(pRes) ? pRes : (pRes?.data ?? []))
    } catch (err) {
      setError(err?.message || 'Không thể tải dữ liệu ca')
    } finally { setLoadingShifts(false) }
  }

  useEffect(() => {
    if (tab === 'calendar') loadCalendar()
    else loadShifts()
  }, [groupId, weekStart, tab])

  /* ───── Calendar by date ───── */
  const calByDate = useMemo(() => {
    const map = {}
    weekDays.forEach(d => { map[fmtISO(d)] = [] })
    calendarItems.forEach(item => {
      const key = item.date
      if (map[key]) map[key].push(item)
      else map[key] = [item]
    })
    return map
  }, [calendarItems, weekDays])

  /* ───── Shifts by date ───── */
  const shiftsByDate = useMemo(() => {
    const map = {}
    weekDays.forEach(d => { map[fmtISO(d)] = [] })
    shifts.forEach(s => {
      const key = s.date
      if (map[key]) map[key].push(s)
      else map[key] = [s]
    })
    return map
  }, [shifts, weekDays])

  function prevWeek() { setWeekStart(addDays(weekStart, -7)) }
  function nextWeek() { setWeekStart(addDays(weekStart, 7)) }
  function goToday() { setWeekStart(startOfWeek(new Date())) }

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  /* ───── Register shift (B12) ───── */
  async function handleRegister(e) {
    e.preventDefault()
    if (!regPos) { setError('Chọn vị trí'); return }
    setRegistering(true); setError(null)
    try {
      await registerShift(regShift.id, Number(regPos), regNote.trim() || null)
      showToast('Đăng ký ca thành công! Chờ Manager duyệt.')
      setRegShift(null); setRegPos(''); setRegNote('')
      if (tab === 'calendar') await loadCalendar()
      else await loadShifts()
    } catch (err) {
      setError(err?.message || 'Không thể đăng ký ca')
    } finally { setRegistering(false) }
  }

  /* ───── Cancel registration (B13) ───── */
  async function handleCancel(e) {
    e.preventDefault()
    setCancelling(true); setError(null)
    try {
      await cancelRegistration(cancelItem.registrationId, cancelReason.trim() || null)
      showToast('Đã hủy đăng ký thành công.')
      setCancelItem(null); setCancelReason('')
      await loadCalendar()
    } catch (err) {
      setError(err?.message || 'Không thể hủy đăng ký')
    } finally { setCancelling(false) }
  }

  const loading = tab === 'calendar' ? loadingCal : loadingShifts

  return (
    <div className="w-full space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">Cá nhân</p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">Lịch làm việc</h2>
          <p className="text-on-surface-variant font-medium">
            {tab === 'calendar' ? 'Xem lịch ca đã đăng ký và trạng thái duyệt' : 'Xem các ca đang mở và đăng ký'}
          </p>
        </div>
        {/* Tab Switcher */}
        <div className="flex bg-surface-container rounded-xl p-1 self-start">
          <button onClick={() => setTab('calendar')}
            className={`px-4 py-2 text-sm font-bold rounded-lg transition-all flex items-center gap-2 ${tab === 'calendar' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:bg-surface-container-high'}`}>
            <span className="material-symbols-outlined text-base">calendar_month</span>
            Lịch của tôi
          </button>
          <button onClick={() => setTab('available')}
            className={`px-4 py-2 text-sm font-bold rounded-lg transition-all flex items-center gap-2 ${tab === 'available' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:bg-surface-container-high'}`}>
            <span className="material-symbols-outlined text-base">event_available</span>
            Ca đang mở
          </button>
        </div>
      </div>

      {/* Week navigation */}
      <div className="flex items-center justify-between bg-surface-container-lowest rounded-2xl border border-outline/10 px-5 py-3 shadow-sm">
        <button onClick={prevWeek} className="p-2 hover:bg-surface-container-high rounded-lg transition-colors">
          <span className="material-symbols-outlined">chevron_left</span>
        </button>
        <div className="flex items-center gap-3">
          <button onClick={goToday}
            className="px-3 py-1.5 text-xs font-bold text-primary bg-primary-container/30 rounded-lg hover:bg-primary-container/50 transition-colors">
            Hôm nay
          </button>
          <span className="text-base font-bold text-on-surface">
            {MONTH_NAMES[weekStart.getMonth()]} {weekStart.getDate()} — {MONTH_NAMES[weekEnd.getMonth()]} {weekEnd.getDate()}, {weekEnd.getFullYear()}
          </span>
        </div>
        <button onClick={nextWeek} className="p-2 hover:bg-surface-container-high rounded-lg transition-colors">
          <span className="material-symbols-outlined">chevron_right</span>
        </button>
      </div>

      {error && <div className="bg-error-container/20 text-on-error-container rounded-xl p-4 text-center">{error}</div>}
      {loading && <div className="text-center py-12"><p className="text-on-surface-variant animate-pulse">Đang tải...</p></div>}

      {/* ═══ TAB: My Calendar (B19) ═══ */}
      {!loading && tab === 'calendar' && (
        <>
          {/* Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
              <p className="text-3xl font-black text-on-surface">{calendarItems.length}</p>
              <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Tổng đăng ký</p>
            </div>
            <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
              <p className="text-3xl font-black text-emerald-600">{calendarItems.filter(i => i.registrationStatus === 'APPROVED').length}</p>
              <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Đã duyệt</p>
            </div>
            <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
              <p className="text-3xl font-black text-amber-600">{calendarItems.filter(i => i.registrationStatus === 'PENDING').length}</p>
              <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Chờ duyệt</p>
            </div>
            <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
              <p className="text-3xl font-black text-red-600">{calendarItems.filter(i => i.registrationStatus === 'REJECTED').length}</p>
              <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Từ chối</p>
            </div>
          </div>

          {/* Week Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-7 gap-3">
            {weekDays.map((day, idx) => {
              const key = fmtISO(day)
              const items = calByDate[key] || []
              const isToday = key === fmtISO(new Date())
              return (
                <div key={key}
                  className={`rounded-2xl border transition-all flex flex-col ${
                    isToday ? 'bg-primary-container/10 border-primary/20 shadow-md ring-1 ring-primary/10'
                    : 'bg-surface-container-lowest border-outline/10 shadow-sm'
                  }`}>
                  <div className={`px-3 py-3 border-b flex items-center gap-2 ${isToday ? 'border-primary/10' : 'border-outline/5'}`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-black ${isToday ? 'bg-primary text-on-primary' : 'text-on-surface'}`}>
                      {day.getDate()}
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">{DAY_LABELS[idx]}</span>
                  </div>
                  <div className="flex-1 p-2 space-y-2 min-h-[100px]">
                    {items.length === 0 && (
                      <div className="flex items-center justify-center h-full">
                        <p className="text-[10px] text-on-surface-variant opacity-30">Trống</p>
                      </div>
                    )}
                    {items.map(item => {
                      const st = REG_STATUS_CFG[item.registrationStatus] || REG_STATUS_CFG.PENDING
                      return (
                        <div key={item.registrationId}
                          className="rounded-xl border border-outline/10 p-2.5 bg-white/50 hover:shadow-sm transition-all">
                          <div className="flex items-center gap-1.5 mb-1">
                            <div className="w-3 h-3 rounded flex-shrink-0"
                              style={{ backgroundColor: item.positionColorCode || '#6366f1' }} />
                            <span className="text-xs font-bold text-on-surface truncate">{item.shiftName || 'Ca'}</span>
                          </div>
                          <p className="text-[10px] text-on-surface-variant font-medium mb-1.5 pl-[18px]">
                            {fmtTime(item.startTime)} – {fmtTime(item.endTime)}
                          </p>
                          <div className="flex items-center justify-between">
                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-bold ${st.cls}`}>
                              <span className="material-symbols-outlined text-[10px]">{st.icon}</span>
                              {st.label}
                            </span>
                            {(item.registrationStatus === 'PENDING' || item.registrationStatus === 'APPROVED') && (
                              <button onClick={() => { setCancelItem(item); setCancelReason('') }}
                                className="p-1 text-on-surface-variant hover:text-error hover:bg-error-container/20 rounded transition-all"
                                title="Hủy đăng ký">
                                <span className="material-symbols-outlined text-sm">close</span>
                              </button>
                            )}
                          </div>
                          {item.positionName && (
                            <p className="text-[9px] text-on-surface-variant mt-1 pl-[18px] truncate">📋 {item.positionName}</p>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>

          {calendarItems.length === 0 && (
            <div className="text-center py-12 bg-surface-container-lowest rounded-2xl border border-outline/10 border-dashed">
              <span className="material-symbols-outlined text-5xl text-on-surface-variant opacity-20 mb-4">event_busy</span>
              <h3 className="text-xl font-bold text-on-surface mb-2">Chưa có lịch</h3>
              <p className="text-on-surface-variant font-medium">Chuyển sang tab "Ca đang mở" để đăng ký ca làm việc.</p>
            </div>
          )}
        </>
      )}

      {/* ═══ TAB: Available Shifts (B11 + B12) ═══ */}
      {!loading && tab === 'available' && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-7 gap-3">
            {weekDays.map((day, idx) => {
              const key = fmtISO(day)
              const dayShifts = (shiftsByDate[key] || []).filter(s => s.status === 'OPEN')
              const isToday = key === fmtISO(new Date())
              return (
                <div key={key}
                  className={`rounded-2xl border transition-all flex flex-col ${
                    isToday ? 'bg-primary-container/10 border-primary/20 shadow-md ring-1 ring-primary/10'
                    : 'bg-surface-container-lowest border-outline/10 shadow-sm'
                  }`}>
                  <div className={`px-3 py-3 border-b flex items-center gap-2 ${isToday ? 'border-primary/10' : 'border-outline/5'}`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-black ${isToday ? 'bg-primary text-on-primary' : 'text-on-surface'}`}>
                      {day.getDate()}
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">{DAY_LABELS[idx]}</span>
                  </div>
                  <div className="flex-1 p-2 space-y-2 min-h-[100px]">
                    {dayShifts.length === 0 && (
                      <div className="flex items-center justify-center h-full">
                        <p className="text-[10px] text-on-surface-variant opacity-30">Không có ca mở</p>
                      </div>
                    )}
                    {dayShifts.map(shift => {
                      const shiftReqs = shift.requirements || []
                      return (
                        <div key={shift.id}
                          className="rounded-xl border border-emerald-200 bg-emerald-50 p-2.5 hover:shadow-sm transition-all">
                          <div className="flex items-center gap-1.5 mb-1">
                            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 flex-shrink-0" />
                            <span className="text-xs font-bold text-on-surface truncate">{shift.name || 'Ca'}</span>
                          </div>
                          <p className="text-[10px] text-on-surface-variant font-medium mb-1.5 pl-3">
                            {fmtTime(shift.startTime)} – {fmtTime(shift.endTime)}
                          </p>
                          {shiftReqs.length > 0 && (
                            <div className="space-y-1 mb-1.5">
                              {shiftReqs.map(req => (
                                <div key={req.id} className="flex items-center gap-1.5">
                                  <div className="w-3 h-3 rounded flex-shrink-0"
                                    style={{ backgroundColor: req.positionColorCode || '#6366f1' }} />
                                  <span className="text-[10px] text-on-surface truncate flex-1">{req.positionName}</span>
                                  <span className="text-[10px] font-bold text-on-surface-variant">{req.quantity}</span>
                                </div>
                              ))}
                            </div>
                          )}
                          <button onClick={() => { setRegShift(shift); setRegPos(''); setRegNote('') }}
                            className="w-full mt-1 py-1.5 text-[10px] font-bold text-primary bg-primary-container/20 hover:bg-primary-container/40 rounded-lg transition-colors flex items-center justify-center gap-1">
                            <span className="material-symbols-outlined text-[12px]">how_to_reg</span>
                            Đăng ký ca
                          </button>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>

          {shifts.filter(s => s.status === 'OPEN').length === 0 && (
            <div className="text-center py-12 bg-surface-container-lowest rounded-2xl border border-outline/10 border-dashed">
              <span className="material-symbols-outlined text-5xl text-on-surface-variant opacity-20 mb-4">event_busy</span>
              <h3 className="text-xl font-bold text-on-surface mb-2">Không có ca đang mở</h3>
              <p className="text-on-surface-variant font-medium">Chưa có ca nào đang mở đăng ký trong tuần này.</p>
            </div>
          )}
        </>
      )}

      {/* ═══ Register Shift Modal (B12) ═══ */}
      {regShift && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center modal-overlay" onClick={() => setRegShift(null)}>
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-md mx-4 animate-[fadeIn_0.2s_ease-out]"
            onClick={e => e.stopPropagation()}>
            <div className="px-6 pt-6 pb-4 border-b border-outline/10 flex items-center justify-between">
              <h3 className="text-xl font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">how_to_reg</span>
                Đăng ký ca
              </h3>
              <button onClick={() => setRegShift(null)} className="p-1.5 text-on-surface-variant hover:bg-surface-container-high rounded-lg">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleRegister} className="p-6 space-y-5">
              {/* Shift info */}
              <div className="bg-surface-container rounded-xl p-4">
                <p className="text-sm font-bold text-on-surface">{regShift.name || 'Ca'}</p>
                <p className="text-xs text-on-surface-variant mt-1">{regShift.date} · {fmtTime(regShift.startTime)} – {fmtTime(regShift.endTime)}</p>
              </div>

              {/* Position selection */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Vị trí đăng ký <span className="text-error">*</span>
                </label>
                <select value={regPos} onChange={e => setRegPos(e.target.value)}
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all">
                  <option value="">— Chọn vị trí —</option>
                  {positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  {(regShift.requirements || []).map(req => (
                    !positions.find(p => p.id === req.positionId)
                      ? <option key={req.positionId} value={req.positionId}>{req.positionName}</option>
                      : null
                  ))}
                </select>
              </div>

              {/* Note */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Ghi chú</label>
                <textarea value={regNote} onChange={e => setRegNote(e.target.value)}
                  placeholder="Ghi chú cho Manager (tùy chọn)..." rows={2}
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all resize-none" />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setRegShift(null)}
                  className="px-5 py-2.5 text-on-surface-variant font-semibold rounded-lg hover:bg-surface-container-high transition-colors">Hủy</button>
                <button type="submit" disabled={registering}
                  className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors shadow-md disabled:opacity-50">
                  {registering ? 'Đang đăng ký...' : 'Xác nhận đăng ký'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ═══ Cancel Registration Modal (B13) ═══ */}
      {cancelItem && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center modal-overlay" onClick={() => setCancelItem(null)}>
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-md mx-4 animate-[fadeIn_0.2s_ease-out]"
            onClick={e => e.stopPropagation()}>
            <div className="px-6 pt-6 pb-4 border-b border-outline/10 flex items-center justify-between">
              <h3 className="text-xl font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-error">event_busy</span>
                Hủy đăng ký ca
              </h3>
              <button onClick={() => setCancelItem(null)} className="p-1.5 text-on-surface-variant hover:bg-surface-container-high rounded-lg">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleCancel} className="p-6 space-y-5">
              <div className="bg-error-container/10 border border-error/20 rounded-xl p-4">
                <p className="text-sm font-bold text-on-surface">{cancelItem.shiftName || 'Ca'}</p>
                <p className="text-xs text-on-surface-variant mt-1">{cancelItem.date} · {fmtTime(cancelItem.startTime)} – {fmtTime(cancelItem.endTime)}</p>
                {cancelItem.positionName && (
                  <p className="text-xs text-on-surface-variant mt-1">📋 {cancelItem.positionName}</p>
                )}
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 text-sm text-amber-800 flex items-start gap-2">
                <span className="material-symbols-outlined text-base mt-0.5">warning</span>
                <span>Sau khi hủy, bạn có thể cần đăng ký lại và chờ duyệt.</span>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Lý do hủy</label>
                <textarea value={cancelReason} onChange={e => setCancelReason(e.target.value)}
                  placeholder="Nhập lý do (tùy chọn)..." rows={2}
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all resize-none" />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setCancelItem(null)}
                  className="px-5 py-2.5 text-on-surface-variant font-semibold rounded-lg hover:bg-surface-container-high transition-colors">Quay lại</button>
                <button type="submit" disabled={cancelling}
                  className="px-5 py-2.5 bg-error text-on-error font-semibold rounded-lg hover:bg-error/90 transition-colors shadow-md disabled:opacity-50">
                  {cancelling ? 'Đang hủy...' : 'Xác nhận hủy'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toast notification */}
      {toast && (
        <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 z-[200] shadow-2xl border rounded-2xl px-6 py-4 flex items-center gap-3 animate-[fadeIn_0.2s_ease-out] max-w-md ${
          toast.type === 'success' ? 'bg-surface border-emerald-200' : 'bg-surface border-error/20'
        }`}>
          <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
            toast.type === 'success' ? 'bg-emerald-100' : 'bg-error-container/30'
          }`}>
            <span className={`material-symbols-outlined ${
              toast.type === 'success' ? 'text-emerald-600' : 'text-error'
            }`}>{toast.type === 'success' ? 'check_circle' : 'error'}</span>
          </div>
          <p className="text-sm font-medium text-on-surface">{toast.msg}</p>
          <button onClick={() => setToast(null)} className="p-1 text-on-surface-variant hover:text-on-surface flex-shrink-0">
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>
      )}
    </div>
  )
}
