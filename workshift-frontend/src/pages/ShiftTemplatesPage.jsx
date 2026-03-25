import { useEffect, useState } from 'react'
import { useParams, useOutletContext } from 'react-router-dom'
import { getTemplates, createTemplate, updateTemplate, deleteTemplate } from '../features/shifts/shiftTemplateApi'
import { getPositions } from '../features/positions/positionApi'

export function ShiftTemplatesPage() {
  const { groupId } = useParams()
  const { isManager } = useOutletContext() || {}
  const [templates, setTemplates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Form state
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formName, setFormName] = useState('')
  const [formStart, setFormStart] = useState('')
  const [formEnd, setFormEnd] = useState('')
  const [formDesc, setFormDesc] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  // positions + requirements
  const [positions, setPositions] = useState([])
  const [formReqs, setFormReqs] = useState([]) // [{positionId, quantity}]

  async function loadTemplates() {
    setLoading(true)
    setError(null)
    try {
      const res = await getTemplates(groupId)
      const list = Array.isArray(res) ? res : (res?.data ?? [])
      setTemplates(list)
    } catch (err) {
      setError(err?.message || 'Không thể tải danh sách ca mẫu')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadTemplates() }, [groupId])

  useEffect(() => {
    if (!groupId) return
    getPositions(groupId).then(res => {
      setPositions(Array.isArray(res) ? res : (res?.data ?? []))
    }).catch(() => {})
  }, [groupId])

  function formatTime(t) {
    if (!t) return '—'
    // handle "HH:mm:ss" or "HH:mm"
    return String(t).substring(0, 5)
  }

  function calcDuration(start, end) {
    if (!start || !end) return null
    const [sh, sm] = start.split(':').map(Number)
    const [eh, em] = end.split(':').map(Number)
    const mins = (eh * 60 + em) - (sh * 60 + sm)
    if (mins <= 0) return null
    const h = Math.floor(mins / 60)
    const m = mins % 60
    return m > 0 ? `${h}h${m}p` : `${h}h`
  }

  function openCreate() {
    setEditingId(null)
    setFormName('')
    setFormStart('')
    setFormEnd('')
    setFormDesc('')
    setFormError(null)
    setFormReqs([])
    setShowForm(true)
  }

  function openEdit(tpl) {
    setEditingId(tpl.id)
    setFormName(tpl.name)
    setFormStart(formatTime(tpl.startTime))
    setFormEnd(formatTime(tpl.endTime))
    setFormDesc(tpl.description || '')
    setFormError(null)
    setFormReqs((tpl.requirements || []).map(r => ({ positionId: r.positionId, quantity: r.quantity })))
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditingId(null)
    setFormError(null)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!formName.trim()) { setFormError('Tên ca mẫu không được để trống'); return }
    if (!formStart || !formEnd) { setFormError('Giờ bắt đầu và kết thúc là bắt buộc'); return }
    setSubmitting(true)
    setFormError(null)
    try {
      const payload = {
        name: formName.trim(),
        startTime: formStart + ':00',
        endTime: formEnd + ':00',
        description: formDesc.trim() || null,
        requirements: formReqs.filter(r => r.positionId && r.quantity > 0),
      }
      if (editingId) {
        await updateTemplate(groupId, editingId, payload)
      } else {
        await createTemplate(groupId, payload)
      }
      closeForm()
      await loadTemplates()
    } catch (err) {
      setFormError(err?.message || 'Có lỗi xảy ra')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(tpl) {
    if (!confirm(`Xóa ca mẫu "${tpl.name}"?`)) return
    try {
      await deleteTemplate(groupId, tpl.id)
      await loadTemplates()
    } catch (err) {
      alert(err?.message || 'Không thể xóa')
    }
  }

  const SHIFT_ICONS = ['light_mode', 'wb_sunny', 'wb_twilight', 'dark_mode', 'schedule']

  function getShiftIcon(name) {
    const lower = (name || '').toLowerCase()
    if (lower.includes('sáng') || lower.includes('sang') || lower.includes('morning')) return 'light_mode'
    if (lower.includes('trưa') || lower.includes('trua') || lower.includes('noon')) return 'wb_sunny'
    if (lower.includes('chiều') || lower.includes('chieu') || lower.includes('afternoon')) return 'wb_twilight'
    if (lower.includes('tối') || lower.includes('toi') || lower.includes('night') || lower.includes('đêm')) return 'dark_mode'
    return 'schedule'
  }

  const SHIFT_COLORS = {
    light_mode: { bg: 'bg-amber-50', text: 'text-amber-600', border: 'border-amber-200', accent: '#f59e0b' },
    wb_sunny: { bg: 'bg-orange-50', text: 'text-orange-600', border: 'border-orange-200', accent: '#f97316' },
    wb_twilight: { bg: 'bg-purple-50', text: 'text-purple-600', border: 'border-purple-200', accent: '#a855f7' },
    dark_mode: { bg: 'bg-indigo-50', text: 'text-indigo-600', border: 'border-indigo-200', accent: '#6366f1' },
    schedule: { bg: 'bg-sky-50', text: 'text-sky-600', border: 'border-sky-200', accent: '#0ea5e9' },
  }

  return (
    <div className="w-full space-y-8">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">Cấu hình</p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">Ca mẫu</h2>
          <p className="text-on-surface-variant font-medium">Tạo các khung giờ mẫu để tạo ca nhanh hơn (Ca Sáng, Ca Chiều...)</p>
        </div>
        {isManager && (
          <button onClick={openCreate}
            className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors flex items-center gap-2 self-start shadow-md">
            <span className="material-symbols-outlined text-sm">add</span>
            <span>Thêm ca mẫu</span>
          </button>
        )}
      </div>

      {/* Loading */}
      {loading && (
        <div className="text-center py-12">
          <p className="text-on-surface-variant animate-pulse">Đang tải...</p>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-error-container/20 text-on-error-container rounded-xl p-4 text-center">{error}</div>
      )}

      {/* Template Cards */}
      {!loading && !error && templates.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {templates.map((tpl) => {
            const icon = getShiftIcon(tpl.name)
            const colors = SHIFT_COLORS[icon]
            const duration = calcDuration(tpl.startTime, tpl.endTime)
            return (
              <div key={tpl.id}
                className={`bg-surface-container-lowest rounded-2xl border border-outline/10 shadow-[0_24px_48px_rgba(0,52,94,0.06)] hover:shadow-lg transition-all group overflow-hidden`}>
                {/* Color strip */}
                <div className="h-1" style={{ backgroundColor: colors.accent }} />
                <div className="p-5">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${colors.bg} ${colors.text}`}>
                        <span className="material-symbols-outlined">{icon}</span>
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-on-surface">{tpl.name}</h3>
                        {duration && <span className="text-xs text-on-surface-variant">{duration}</span>}
                      </div>
                    </div>
                    {isManager && (
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onClick={() => openEdit(tpl)} title="Sửa"
                          className="p-1.5 text-on-surface-variant hover:bg-primary-container/30 rounded-lg transition-colors">
                          <span className="material-symbols-outlined text-lg">edit</span>
                        </button>
                        <button onClick={() => handleDelete(tpl)} title="Xóa"
                          className="p-1.5 text-on-surface-variant hover:bg-error-container/30 hover:text-error rounded-lg transition-colors">
                          <span className="material-symbols-outlined text-lg">delete</span>
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Time display */}
                  <div className={`rounded-xl p-3 ${colors.bg} ${colors.border} border`}>
                    <div className="flex items-center justify-between">
                      <div className="text-center">
                        <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant opacity-60">Bắt đầu</p>
                        <p className={`text-lg font-black ${colors.text}`}>{formatTime(tpl.startTime)}</p>
                      </div>
                      <span className="material-symbols-outlined text-on-surface-variant opacity-30">arrow_forward</span>
                      <div className="text-center">
                        <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant opacity-60">Kết thúc</p>
                        <p className={`text-lg font-black ${colors.text}`}>{formatTime(tpl.endTime)}</p>
                      </div>
                    </div>
                  </div>

                  {tpl.description && (
                    <p className="text-sm text-on-surface-variant mt-3 line-clamp-2">{tpl.description}</p>
                  )}

                  {/* Requirements display */}
                  {(tpl.requirements && tpl.requirements.length > 0) && (
                    <div className="mt-3 pt-3 border-t border-outline/10">
                      <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-2">Nhu cầu mặc định</p>
                      <div className="flex flex-wrap gap-1.5">
                        {tpl.requirements.map(req => (
                          <span key={req.id} className="inline-flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-lg border border-outline/10 bg-surface-container">
                            <span className="w-2 h-2 rounded-full" style={{ backgroundColor: req.positionColorCode || '#6366f1' }}></span>
                            {req.positionName} ×{req.quantity}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && templates.length === 0 && (
        <div className="text-center py-16 bg-surface-container-lowest rounded-2xl border border-outline/10 border-dashed">
          <span className="material-symbols-outlined text-5xl text-on-surface-variant opacity-20 mb-4">schedule</span>
          <h3 className="text-xl font-bold text-on-surface mb-2">Chưa có ca mẫu nào</h3>
          <p className="text-on-surface-variant font-medium">Tạo khung giờ mẫu để tạo ca làm việc nhanh hơn.</p>
        </div>
      )}

      {/* Modal Form */}
      {showForm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center modal-overlay" onClick={closeForm}>
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-md mx-4 p-0 animate-[fadeIn_0.2s_ease-out]"
            onClick={(e) => e.stopPropagation()}>
            <div className="px-6 pt-6 pb-4 border-b border-outline/10">
              <h3 className="text-xl font-bold text-on-surface">
                {editingId ? 'Chỉnh sửa ca mẫu' : 'Thêm ca mẫu mới'}
              </h3>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-5">
              {formError && (
                <div className="bg-error-container/20 text-on-error-container rounded-lg p-3 text-sm">{formError}</div>
              )}
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Tên ca mẫu <span className="text-error">*</span>
                </label>
                <input type="text" value={formName} onChange={(e) => setFormName(e.target.value)}
                  placeholder="VD: Ca Sáng, Ca Chiều, Ca Tối..."
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                  autoFocus />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                    Bắt đầu <span className="text-error">*</span>
                  </label>
                  <input type="time" value={formStart} onChange={(e) => setFormStart(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                    Kết thúc <span className="text-error">*</span>
                  </label>
                  <input type="time" value={formEnd} onChange={(e) => setFormEnd(e.target.value)}
                    className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Mô tả
                </label>
                <textarea value={formDesc} onChange={(e) => setFormDesc(e.target.value)}
                  placeholder="Mô tả ca mẫu (tùy chọn)..."
                  rows={3}
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all resize-none" />
              </div>

              {/* Requirements section */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Nhu cầu nhân sự mặc định
                </label>
                <div className="space-y-2">
                  {formReqs.map((req, idx) => (
                    <div key={idx} className="flex items-center gap-2">
                      <select value={req.positionId} onChange={e => {
                        const updated = [...formReqs]
                        updated[idx].positionId = Number(e.target.value)
                        setFormReqs(updated)
                      }}
                        className="flex-1 px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline/20 text-on-surface text-sm focus:outline-none focus:border-primary">
                        <option value="">Chọn vị trí</option>
                        {positions.map(p => (
                          <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                      </select>
                      <input type="number" min="1" value={req.quantity} onChange={e => {
                        const updated = [...formReqs]
                        updated[idx].quantity = Number(e.target.value)
                        setFormReqs(updated)
                      }}
                        className="w-16 px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline/20 text-on-surface text-sm text-center focus:outline-none focus:border-primary" />
                      <button type="button" onClick={() => setFormReqs(formReqs.filter((_, i) => i !== idx))}
                        className="p-1.5 text-error hover:bg-error/10 rounded-lg transition-colors">
                        <span className="material-symbols-outlined text-sm">close</span>
                      </button>
                    </div>
                  ))}
                  <button type="button" onClick={() => setFormReqs([...formReqs, { positionId: '', quantity: 1 }])}
                    className="w-full px-3 py-2 border border-dashed border-outline/30 rounded-lg text-xs font-bold text-primary hover:bg-primary/5 transition-colors flex items-center justify-center gap-1">
                    <span className="material-symbols-outlined text-sm">add</span>
                    Thêm nhu cầu
                  </button>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={closeForm}
                  className="px-5 py-2.5 text-on-surface-variant font-semibold rounded-lg hover:bg-surface-container-high transition-colors">
                  Hủy
                </button>
                <button type="submit" disabled={submitting}
                  className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors shadow-md disabled:opacity-50">
                  {submitting ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Tạo mới')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
