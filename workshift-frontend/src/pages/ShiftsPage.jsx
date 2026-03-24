import { useEffect, useState, useMemo } from 'react'
import { useParams, useOutletContext } from 'react-router-dom'
import { getTemplates } from '../features/shifts/shiftTemplateApi'
import { getShifts, createShift } from '../features/shifts/shiftApi'
import { getPositions } from '../features/positions/positionApi'
import { getRequirements, createRequirement, deleteRequirement } from '../features/shifts/shiftRequirementApi'

/* ───── date helpers ───── */
function startOfWeek(d) {
  const dt = new Date(d)
  const day = dt.getDay()
  const diff = day === 0 ? -6 : 1 - day
  dt.setDate(dt.getDate() + diff)
  dt.setHours(0, 0, 0, 0)
  return dt
}
function addDays(d, n) { const r = new Date(d); r.setDate(r.getDate() + n); return r }
function fmtISO(d) { return d.toISOString().slice(0, 10) }
function fmtTime(t) { return t ? String(t).substring(0, 5) : '—' }
function isToday(d) { return fmtISO(d) === fmtISO(new Date()) }

const DAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'CN']
const MONTH_NAMES = ['Th01', 'Th02', 'Th03', 'Th04', 'Th05', 'Th06', 'Th07', 'Th08', 'Th09', 'Th10', 'Th11', 'Th12']

const STATUS_CFG = {
  OPEN: { label: 'Mở', cls: 'bg-emerald-50 border-emerald-200', dot: 'bg-emerald-500', badge: 'bg-emerald-100 text-emerald-700' },
  LOCKED: { label: 'Khóa', cls: 'bg-amber-50 border-amber-200', dot: 'bg-amber-500', badge: 'bg-amber-100 text-amber-700' },
  COMPLETED: { label: 'Xong', cls: 'bg-slate-50 border-slate-200', dot: 'bg-slate-400', badge: 'bg-slate-100 text-slate-600' },
}

export function ShiftsPage() {
  const { groupId } = useParams()
  const { isManager } = useOutletContext() || {}

  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()))
  const [shifts, setShifts] = useState([])
  const [templates, setTemplates] = useState([])
  const [positions, setPositions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // create form
  const [showCreate, setShowCreate] = useState(false)
  const [createDate, setCreateDate] = useState('')
  const [formName, setFormName] = useState('')
  const [formStart, setFormStart] = useState('')
  const [formEnd, setFormEnd] = useState('')
  const [formTpl, setFormTpl] = useState('')
  const [formNote, setFormNote] = useState('')
  const [creating, setCreating] = useState(false)
  const [createErr, setCreateErr] = useState(null)

  // requirement panel
  const [selShift, setSelShift] = useState(null)
  const [reqs, setReqs] = useState([])
  const [loadingReqs, setLoadingReqs] = useState(false)
  const [reqPos, setReqPos] = useState('')
  const [reqQty, setReqQty] = useState(1)
  const [addingReq, setAddingReq] = useState(false)
  const [reqErr, setReqErr] = useState(null)

  // coming soon toast
  const [showToast, setShowToast] = useState(false)
  function handleRegisterClick(e) {
    e.stopPropagation()
    setShowToast(true)
    setTimeout(() => setShowToast(false), 3000)
  }

  const weekEnd = useMemo(() => addDays(weekStart, 6), [weekStart])
  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart])

  async function loadData() {
    setLoading(true)
    setError(null)
    try {
      const from = fmtISO(weekStart)
      const to = fmtISO(weekEnd)
      const [sRes, tRes, pRes] = await Promise.all([
        getShifts(groupId, from, to),
        getTemplates(groupId),
        getPositions(groupId),
      ])
      setShifts(Array.isArray(sRes) ? sRes : (sRes?.data ?? []))
      setTemplates(Array.isArray(tRes) ? tRes : (tRes?.data ?? []))
      setPositions(Array.isArray(pRes) ? pRes : (pRes?.data ?? []))
    } catch (err) {
      setError(err?.message || 'Không thể tải dữ liệu')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [groupId, weekStart])

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

  /* ───── create shift ───── */
  function openCreateForDate(dateStr) {
    setCreateDate(dateStr || '')
    setFormName(''); setFormStart(''); setFormEnd(''); setFormTpl(''); setFormNote('')
    setCreateErr(null)
    setShowCreate(true)
  }

  function handleTplChange(v) {
    setFormTpl(v)
    if (v) {
      const t = templates.find(t => String(t.id) === v)
      if (t) { setFormName(t.name || ''); setFormStart(fmtTime(t.startTime)); setFormEnd(fmtTime(t.endTime)) }
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!createDate) { setCreateErr('Chọn ngày'); return }
    if (!formTpl && (!formStart || !formEnd)) { setCreateErr('Nhập giờ bắt đầu và kết thúc'); return }
    setCreating(true); setCreateErr(null)
    try {
      const payload = { name: formName.trim() || null, date: createDate, note: formNote.trim() || null }
      if (formTpl) { payload.templateId = Number(formTpl) }
      else { payload.startTime = formStart + ':00'; payload.endTime = formEnd + ':00' }
      await createShift(groupId, payload)
      setShowCreate(false)
      await loadData()
    } catch (err) { setCreateErr(err?.message || 'Không thể tạo ca') }
    finally { setCreating(false) }
  }

  /* ───── requirements ───── */
  async function openReqs(shift) {
    setSelShift(shift)
    setLoadingReqs(true); setReqErr(null); setReqPos(''); setReqQty(1)
    try {
      const r = await getRequirements(shift.id)
      setReqs(Array.isArray(r) ? r : (r?.data ?? []))
    } catch { setReqs(shift.requirements || []) }
    finally { setLoadingReqs(false) }
  }

  async function handleAddReq(e) {
    e.preventDefault()
    if (!reqPos) { setReqErr('Chọn vị trí'); return }
    setAddingReq(true); setReqErr(null)
    try {
      await createRequirement(selShift.id, { positionId: Number(reqPos), quantity: Number(reqQty) })
      const r = await getRequirements(selShift.id)
      setReqs(Array.isArray(r) ? r : (r?.data ?? []))
      setReqPos(''); setReqQty(1)
      await loadData() // refresh calendar
    } catch (err) { setReqErr(err?.message || 'Lỗi') }
    finally { setAddingReq(false) }
  }

  async function handleDelReq(id) {
    try {
      await deleteRequirement(selShift.id, id)
      setReqs(p => p.filter(r => r.id !== id))
      await loadData()
    } catch (err) { alert(err?.message || 'Lỗi') }
  }

  /* ───── render ───── */
  return (
    <div className="w-full space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">Quản lý</p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">Lịch ca làm việc</h2>
          <p className="text-on-surface-variant font-medium">
            {isManager ? 'Xem tổng quan ca và nhu cầu nhân sự theo tuần' : 'Xem lịch ca và đăng ký ca làm việc'}
          </p>
        </div>
        {isManager && (
          <button onClick={() => openCreateForDate(fmtISO(new Date()))}
            className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors flex items-center gap-2 self-start shadow-md">
            <span className="material-symbols-outlined text-sm">add</span>
            <span>Tạo ca mới</span>
          </button>
        )}
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

      {loading && <div className="text-center py-12"><p className="text-on-surface-variant animate-pulse">Đang tải...</p></div>}
      {error && <div className="bg-error-container/20 text-on-error-container rounded-xl p-4 text-center">{error}</div>}

      {/* ═══ Week Calendar Grid ═══ */}
      {!loading && !error && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-7 gap-3">
          {weekDays.map((day, idx) => {
            const key = fmtISO(day)
            const dayShifts = shiftsByDate[key] || []
            const today = isToday(day)
            return (
              <div key={key}
                className={`rounded-2xl border transition-all flex flex-col ${
                  today
                    ? 'bg-primary-container/10 border-primary/20 shadow-md ring-1 ring-primary/10'
                    : 'bg-surface-container-lowest border-outline/10 shadow-sm'
                }`}>
                {/* Day header */}
                <div className={`px-3 py-3 border-b flex items-center justify-between ${
                  today ? 'border-primary/10' : 'border-outline/5'
                }`}>
                  <div className="flex items-center gap-2">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-black ${
                      today ? 'bg-primary text-on-primary' : 'text-on-surface'
                    }`}>
                      {day.getDate()}
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                      {DAY_LABELS[idx]}
                    </span>
                  </div>
                  {isManager && (
                    <button onClick={() => openCreateForDate(key)} title="Thêm ca"
                      className="w-6 h-6 flex items-center justify-center rounded-full hover:bg-primary-container/40 text-on-surface-variant hover:text-primary transition-colors">
                      <span className="material-symbols-outlined text-base">add</span>
                    </button>
                  )}
                </div>

                {/* Shifts in this day */}
                <div className="flex-1 p-2 space-y-2 min-h-[120px]">
                  {dayShifts.length === 0 && (
                    <div className="flex items-center justify-center h-full">
                      <p className="text-[10px] text-on-surface-variant opacity-30">Trống</p>
                    </div>
                  )}
                  {dayShifts.map(shift => {
                    const st = STATUS_CFG[shift.status] || STATUS_CFG.OPEN
                    const isActive = selShift?.id === shift.id
                    const shiftReqs = shift.requirements || []
                    const totalReq = shift.totalRequired || 0
                    return (
                      <div key={shift.id}
                        onClick={() => openReqs(shift)}
                        className={`cursor-pointer rounded-xl border p-2.5 transition-all ${
                          isActive
                            ? 'border-primary bg-primary/5 ring-1 ring-primary/20 shadow-md'
                            : `${st.cls} hover:shadow-sm`
                        }`}>
                        {/* Shift name + time */}
                        <div className="flex items-center gap-1.5 mb-1">
                          <div className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${st.dot}`} />
                          <span className="text-xs font-bold text-on-surface truncate">{shift.name || 'Ca'}</span>
                        </div>
                        <p className="text-[10px] text-on-surface-variant font-medium mb-2 pl-3">
                          {fmtTime(shift.startTime)} – {fmtTime(shift.endTime)}
                        </p>

                        {/* ─── Requirements inline ─── */}
                        {shiftReqs.length > 0 && (
                          <div className="space-y-1 mb-1.5">
                            {shiftReqs.map(req => (
                              <div key={req.id} className="flex items-center gap-1.5">
                                <div className="w-4 h-4 rounded flex-shrink-0 flex items-center justify-center text-[8px] font-bold text-white"
                                  style={{ backgroundColor: req.positionColorCode || '#6366f1' }}>
                                  {(req.positionName || '?').charAt(0)}
                                </div>
                                <span className="text-[10px] text-on-surface truncate flex-1">{req.positionName}</span>
                                <span className="text-[10px] font-bold text-on-surface-variant">{req.quantity}</span>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Total summary */}
                        {totalReq > 0 && (
                          <div className="flex items-center justify-between pt-1.5 border-t border-outline/10">
                            <span className="text-[9px] font-bold uppercase text-on-surface-variant tracking-wider">Cần</span>
                            <div className="flex items-center gap-1">
                              <span className="material-symbols-outlined text-[12px] text-primary">groups</span>
                              <span className="text-[10px] font-black text-primary">{totalReq}</span>
                            </div>
                          </div>
                        )}

                        {/* Empty reqs hint */}
                        {shiftReqs.length === 0 && totalReq === 0 && (
                          <div className="pt-1 border-t border-outline/10">
                            <p className="text-[9px] text-on-surface-variant opacity-50 italic">Chưa cấu hình nhu cầu</p>
                          </div>
                        )}

                        {/* Staff register button */}
                        {(!isManager && shift.status === 'OPEN') && (
                          <button onClick={handleRegisterClick}
                            className="w-full mt-1.5 py-1.5 text-[10px] font-bold text-primary bg-primary-container/20 hover:bg-primary-container/40 rounded-lg transition-colors flex items-center justify-center gap-1">
                            <span className="material-symbols-outlined text-[12px]">how_to_reg</span>
                            Đăng ký
                          </button>
                        )}

                        {/* Note */}
                        {shift.note && (
                          <p className="text-[9px] text-on-surface-variant truncate mt-1 italic opacity-60">📝 {shift.note}</p>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Coming soon toast */}
      {showToast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-[200] bg-surface shadow-2xl border border-outline/20 rounded-2xl px-6 py-4 flex items-center gap-3 animate-[fadeIn_0.2s_ease-out] max-w-md">
          <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0">
            <span className="material-symbols-outlined text-amber-600">construction</span>
          </div>
          <div>
            <p className="text-sm font-bold text-on-surface">Tính năng đang phát triển</p>
            <p className="text-xs text-on-surface-variant">Đăng ký ca sẽ được triển khai trong bản cập nhật tiếp theo.</p>
          </div>
          <button onClick={() => setShowToast(false)} className="p-1 text-on-surface-variant hover:text-on-surface flex-shrink-0">
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>
      )}

      {/* Stats row */}
      {!loading && !error && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-on-surface">{shifts.length}</p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Tổng ca</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-emerald-600">{shifts.filter(s => s.status === 'OPEN').length}</p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Đang mở</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-amber-600">{shifts.filter(s => s.status === 'LOCKED').length}</p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Đã khóa</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-on-surface">
              {shifts.reduce((s, sh) => s + (sh.totalRequired || 0), 0)}
            </p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Tổng nhu cầu</p>
          </div>
        </div>
      )}

      {/* ═══ Requirement Config Panel ═══ */}
      {selShift && (
        <div className="bg-surface-container-lowest rounded-2xl border border-outline/10 shadow-lg overflow-hidden">
          <div className="px-6 py-4 bg-surface-container-low border-b border-outline/10 flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">groups</span>
                Nhu cầu nhân sự — {selShift.name || 'Ca'}
              </h3>
              <p className="text-xs text-on-surface-variant mt-0.5">
                {selShift.date} · {fmtTime(selShift.startTime)} – {fmtTime(selShift.endTime)}
              </p>
            </div>
            <button onClick={() => setSelShift(null)} className="p-1.5 text-on-surface-variant hover:bg-surface-container-high rounded-lg">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
          <div className="p-6 space-y-5">
            {loadingReqs && <p className="text-on-surface-variant animate-pulse text-center py-4">Đang tải...</p>}

            {!loadingReqs && reqs.length > 0 && (
              <div className="space-y-2">
                {reqs.map(req => (
                  <div key={req.id} className="flex items-center justify-between bg-surface-container rounded-xl px-4 py-3 group">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-xs font-bold"
                        style={{ backgroundColor: req.positionColorCode || '#6366f1' }}>
                        {(req.positionName || '?').charAt(0).toUpperCase()}
                      </div>
                      <span className="text-sm font-bold text-on-surface">{req.positionName || `#${req.positionId}`}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold text-on-surface bg-surface-container-lowest px-3 py-1 rounded-lg">{req.quantity} người</span>
                      {isManager && (
                        <button onClick={() => handleDelReq(req.id)} className="p-1 text-on-surface-variant hover:text-error opacity-0 group-hover:opacity-100 transition-all">
                          <span className="material-symbols-outlined text-lg">close</span>
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                <div className="flex items-center justify-between px-4 py-2 rounded-xl bg-primary-container/10">
                  <span className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Tổng cần</span>
                  <span className="text-sm font-black text-primary">{reqs.reduce((s, r) => s + (r.quantity || 0), 0)} người</span>
                </div>
              </div>
            )}

            {!loadingReqs && reqs.length === 0 && (
              <div className="text-center py-6 bg-surface-container rounded-xl border border-dashed border-outline/20">
                <span className="material-symbols-outlined text-3xl text-on-surface-variant opacity-20 mb-2">person_add</span>
                <p className="text-sm text-on-surface-variant">Chưa cấu hình nhu cầu nhân sự</p>
              </div>
            )}

            {isManager && positions.length > 0 && (
              <form onSubmit={handleAddReq} className="flex items-end gap-3 flex-wrap">
                <div className="flex-1 min-w-[180px]">
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Vị trí</label>
                  <select value={reqPos} onChange={e => setReqPos(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all">
                    <option value="">— Chọn vị trí —</option>
                    {positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                </div>
                <div className="w-28">
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Số lượng</label>
                  <input type="number" min={1} value={reqQty} onChange={e => setReqQty(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
                <button type="submit" disabled={addingReq}
                  className="px-5 py-3 bg-primary text-on-primary font-semibold rounded-xl hover:bg-primary/90 transition-colors shadow-md disabled:opacity-50 flex items-center gap-2">
                  <span className="material-symbols-outlined text-lg">add</span>
                  {addingReq ? 'Đang thêm...' : 'Thêm'}
                </button>
              </form>
            )}
            {reqErr && <div className="bg-error-container/20 text-on-error-container rounded-lg p-3 text-sm">{reqErr}</div>}

            {isManager && positions.length === 0 && (
              <div className="bg-amber-50 text-amber-800 rounded-xl p-4 text-sm flex items-center gap-3">
                <span className="material-symbols-outlined">warning</span>
                <span>Cần tạo vị trí làm việc trước khi cấu hình nhu cầu nhân sự.</span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Create Shift Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-lg mx-4 animate-[fadeIn_0.2s_ease-out]"
            onClick={e => e.stopPropagation()}>
            <div className="px-6 pt-6 pb-4 border-b border-outline/10 flex items-center justify-between">
              <h3 className="text-xl font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">event_available</span>
                Tạo ca làm việc mới
              </h3>
              <button onClick={() => setShowCreate(false)} className="p-1.5 text-on-surface-variant hover:bg-surface-container-high rounded-lg">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-5">
              {createErr && <div className="bg-error-container/20 text-on-error-container rounded-lg p-3 text-sm">{createErr}</div>}

              {templates.length > 0 && (
                <div>
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Ca mẫu (tùy chọn)</label>
                  <select value={formTpl} onChange={e => handleTplChange(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all">
                    <option value="">— Nhập thủ công —</option>
                    {templates.map(t => <option key={t.id} value={t.id}>{t.name} ({fmtTime(t.startTime)} – {fmtTime(t.endTime)})</option>)}
                  </select>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 sm:col-span-1">
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Tên ca</label>
                  <input type="text" value={formName} onChange={e => setFormName(e.target.value)} placeholder="VD: Ca Sáng"
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
                <div className="col-span-2 sm:col-span-1">
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Ngày <span className="text-error">*</span></label>
                  <input type="date" value={createDate} onChange={e => setCreateDate(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
              </div>

              {!formTpl && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Bắt đầu <span className="text-error">*</span></label>
                    <input type="time" value={formStart} onChange={e => setFormStart(e.target.value)}
                      className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Kết thúc <span className="text-error">*</span></label>
                    <input type="time" value={formEnd} onChange={e => setFormEnd(e.target.value)}
                      className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                  </div>
                </div>
              )}

              {formTpl && (
                <div className="bg-primary-container/10 rounded-xl p-3 text-sm text-on-surface-variant flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary text-lg">info</span>
                  Giờ từ ca mẫu: <strong className="text-on-surface">{formStart} – {formEnd}</strong>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">Ghi chú</label>
                <textarea value={formNote} onChange={e => setFormNote(e.target.value)} placeholder="Ghi chú cho nhân viên..." rows={2}
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all resize-none" />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setShowCreate(false)}
                  className="px-5 py-2.5 text-on-surface-variant font-semibold rounded-lg hover:bg-surface-container-high transition-colors">Hủy</button>
                <button type="submit" disabled={creating}
                  className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors shadow-md disabled:opacity-50">
                  {creating ? 'Đang tạo...' : 'Tạo ca'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
