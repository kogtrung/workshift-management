import { useEffect, useState } from 'react'
import { useParams, useOutletContext } from 'react-router-dom'
import { getPositions, createPosition, updatePosition, deletePosition } from '../features/positions/positionApi'

const PRESET_COLORS = [
  '#6366f1', '#8b5cf6', '#a855f7', '#d946ef',
  '#ec4899', '#f43f5e', '#ef4444', '#f97316',
  '#f59e0b', '#eab308', '#84cc16', '#22c55e',
  '#10b981', '#14b8a6', '#06b6d4', '#0ea5e9',
  '#3b82f6', '#2563eb', '#6d28d9', '#78716c',
]

export function PositionsPage() {
  const { groupId } = useParams()
  const { isManager } = useOutletContext() || {}
  const [positions, setPositions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Form state
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formName, setFormName] = useState('')
  const [formColor, setFormColor] = useState(PRESET_COLORS[0])
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  async function loadPositions() {
    setLoading(true)
    setError(null)
    try {
      const res = await getPositions(groupId)
      const list = Array.isArray(res) ? res : (res?.data ?? [])
      setPositions(list)
    } catch (err) {
      setError(err?.message || 'Không thể tải danh sách vị trí')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadPositions() }, [groupId])

  function openCreate() {
    setEditingId(null)
    setFormName('')
    setFormColor(PRESET_COLORS[0])
    setFormError(null)
    setShowForm(true)
  }

  function openEdit(pos) {
    setEditingId(pos.id)
    setFormName(pos.name)
    setFormColor(pos.colorCode || PRESET_COLORS[0])
    setFormError(null)
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditingId(null)
    setFormName('')
    setFormColor(PRESET_COLORS[0])
    setFormError(null)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!formName.trim()) { setFormError('Tên vị trí không được để trống'); return }
    setSubmitting(true)
    setFormError(null)
    try {
      const payload = { name: formName.trim(), colorCode: formColor }
      if (editingId) {
        await updatePosition(groupId, editingId, payload)
      } else {
        await createPosition(groupId, payload)
      }
      closeForm()
      await loadPositions()
    } catch (err) {
      setFormError(err?.message || 'Có lỗi xảy ra')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(pos) {
    if (!confirm(`Xóa vị trí "${pos.name}"?`)) return
    try {
      await deletePosition(groupId, pos.id)
      await loadPositions()
    } catch (err) {
      alert(err?.message || 'Không thể xóa vị trí')
    }
  }

  return (
    <div className="w-full space-y-8">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">Cấu hình</p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">Vị trí làm việc</h2>
          <p className="text-on-surface-variant font-medium">Định nghĩa các vị trí trong quán (Pha chế, Thu ngân, Phục vụ...)</p>
        </div>
        {isManager && (
          <button onClick={openCreate}
            className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors flex items-center gap-2 self-start shadow-md">
            <span className="material-symbols-outlined text-sm">add</span>
            <span>Thêm vị trí</span>
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

      {/* Position Grid */}
      {!loading && !error && positions.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {positions.map((pos) => (
            <div key={pos.id}
              className="bg-surface-container-lowest rounded-2xl p-5 border border-outline/10 shadow-[0_24px_48px_rgba(0,52,94,0.06)] hover:shadow-lg transition-all group">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold text-sm shadow-sm"
                    style={{ backgroundColor: pos.colorCode || '#6366f1' }}>
                    {pos.name?.charAt(0)?.toUpperCase() || 'P'}
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-on-surface">{pos.name}</h3>
                    <p className="text-xs text-on-surface-variant font-mono">{pos.colorCode || '—'}</p>
                  </div>
                </div>
                {isManager && (
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button onClick={() => openEdit(pos)} title="Sửa"
                      className="p-1.5 text-on-surface-variant hover:bg-primary-container/30 rounded-lg transition-colors">
                      <span className="material-symbols-outlined text-lg">edit</span>
                    </button>
                    <button onClick={() => handleDelete(pos)} title="Xóa"
                      className="p-1.5 text-on-surface-variant hover:bg-error-container/30 hover:text-error rounded-lg transition-colors">
                      <span className="material-symbols-outlined text-lg">delete</span>
                    </button>
                  </div>
                )}
              </div>
              <div className="h-1.5 rounded-full" style={{ backgroundColor: pos.colorCode || '#6366f1', opacity: 0.3 }} />
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && positions.length === 0 && (
        <div className="text-center py-16 bg-surface-container-lowest rounded-2xl border border-outline/10 border-dashed">
          <span className="material-symbols-outlined text-5xl text-on-surface-variant opacity-20 mb-4">work</span>
          <h3 className="text-xl font-bold text-on-surface mb-2">Chưa có vị trí nào</h3>
          <p className="text-on-surface-variant font-medium">Tạo các vị trí làm việc để bắt đầu cấu hình ca.</p>
        </div>
      )}

      {/* Modal Form */}
      {showForm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center modal-overlay" onClick={closeForm}>
          <div className="bg-surface rounded-2xl shadow-2xl w-full max-w-md mx-4 p-0 animate-[fadeIn_0.2s_ease-out]"
            onClick={(e) => e.stopPropagation()}>
            <div className="px-6 pt-6 pb-4 border-b border-outline/10">
              <h3 className="text-xl font-bold text-on-surface">
                {editingId ? 'Chỉnh sửa vị trí' : 'Thêm vị trí mới'}
              </h3>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-5">
              {formError && (
                <div className="bg-error-container/20 text-on-error-container rounded-lg p-3 text-sm">{formError}</div>
              )}
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Tên vị trí <span className="text-error">*</span>
                </label>
                <input type="text" value={formName} onChange={(e) => setFormName(e.target.value)}
                  placeholder="VD: Pha chế, Thu ngân, Phục vụ..."
                  className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
                  autoFocus />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
                  Màu hiển thị
                </label>
                <div className="flex flex-wrap gap-2 mb-3">
                  {PRESET_COLORS.map((c) => (
                    <button key={c} type="button" onClick={() => setFormColor(c)}
                      className={`w-8 h-8 rounded-lg transition-all ${formColor === c ? 'ring-2 ring-primary ring-offset-2 ring-offset-surface scale-110' : 'hover:scale-105'}`}
                      style={{ backgroundColor: c }} />
                  ))}
                </div>
                <div className="flex items-center gap-3">
                  <input type="color" value={formColor} onChange={(e) => setFormColor(e.target.value)}
                    className="w-10 h-10 rounded-lg cursor-pointer border-0 bg-transparent" />
                  <input type="text" value={formColor} onChange={(e) => setFormColor(e.target.value)}
                    className="flex-1 px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline/20 text-sm font-mono text-on-surface focus:outline-none focus:border-primary transition-all" />
                  <div className="w-10 h-10 rounded-lg shadow-inner border border-outline/10" style={{ backgroundColor: formColor }} />
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
