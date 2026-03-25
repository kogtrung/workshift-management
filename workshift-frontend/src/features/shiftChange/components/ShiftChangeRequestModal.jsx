import { useEffect, useMemo, useState } from "react"
import { Modal } from "../../../components/Modal"
import { createShiftChangeRequest } from "../shiftChangeApi"

function fmtTime(t) {
  if (!t) return "—"
  return String(t).substring(0, 5)
}

function normalizeDateTimeKey(shiftLike) {
  // Dùng (date + startTime + endTime) để tránh trùng ca hiện tại.
  const date = shiftLike?.date ? String(shiftLike.date) : null
  const start = shiftLike?.startTime ? fmtTime(shiftLike.startTime) : null
  const end = shiftLike?.endTime ? fmtTime(shiftLike.endTime) : null
  return date && start && end ? `${date}|${start}|${end}` : null
}

export function ShiftChangeRequestModal({
  open,
  onClose,
  groupId,
  fromItem,
  availableShifts,
  onCreated,
}) {
  const [toShiftId, setToShiftId] = useState("")
  const [toPositionId, setToPositionId] = useState("")
  const [reason, setReason] = useState("")
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const eligibleToShifts = useMemo(() => {
    if (!Array.isArray(availableShifts)) return []
    const currentKey = normalizeDateTimeKey(fromItem)
    return availableShifts
      .filter((s) => s?.status === "OPEN")
      .filter((s) => normalizeDateTimeKey(s) !== currentKey)
  }, [availableShifts, fromItem])

  const selectedShift = useMemo(() => {
    if (!toShiftId) return null
    const idNum = Number(toShiftId)
    return eligibleToShifts.find((s) => Number(s.id) === idNum) || null
  }, [toShiftId, eligibleToShifts])

  const positionOptions = useMemo(() => {
    const reqs = selectedShift?.requirements || []
    return reqs.map((r) => ({
      positionId: r.positionId,
      positionName: r.positionName || `#${r.positionId}`,
    }))
  }, [selectedShift])

  useEffect(() => {
    if (!open) return

    setError(null)
    setReason("")
    setToPositionId("")

    const first = eligibleToShifts[0]
    setToShiftId(first ? String(first.id) : "")
  }, [open, eligibleToShifts])

  useEffect(() => {
    // Khi đổi ca đích, reset vị trí đích về mặc định đầu tiên.
    if (!open) return
    if (!positionOptions.length) {
      setToPositionId("")
      return
    }
    const firstPosId = String(positionOptions[0].positionId)
    setToPositionId((prev) => (prev ? prev : firstPosId))
  }, [open, positionOptions])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!fromItem?.registrationId) {
      setError("Không tìm thấy ca đăng ký gốc")
      return
    }
    if (!toShiftId) {
      setError("Chọn ca đích")
      return
    }
    if (!toPositionId) {
      setError("Chọn vị trí đích")
      return
    }

    setSaving(true)
    setError(null)
    try {
      await createShiftChangeRequest(groupId, {
        fromRegistrationId: Number(fromItem.registrationId),
        toShiftId: Number(toShiftId),
        toPositionId: Number(toPositionId),
        reason: reason.trim() || null,
      })
      onCreated?.()
    } catch (e) {
      setError(e?.message || "Không thể tạo yêu cầu đổi ca")
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} maxWidthClass="max-w-2xl">
      <div className="px-6 pt-6 pb-4 border-b border-outline/10 flex items-start justify-between gap-4">
        <div>
          <h3 className="text-xl font-bold text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">swap_horiz</span>
            Yêu cầu đổi ca
          </h3>
          <p className="text-xs text-on-surface-variant mt-1">
            Từ: {fromItem?.date} · {fmtTime(fromItem?.startTime)} - {fmtTime(fromItem?.endTime)}
          </p>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 text-on-surface-variant hover:bg-surface-container-high rounded-lg"
          type="button"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </div>

      <form className="p-6 space-y-4" onSubmit={handleSubmit}>
        {error ? (
          <div className="bg-error-container/20 text-on-error-container rounded-xl p-3 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-error">error</span>
            {error}
          </div>
        ) : null}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
              Ca đích <span className="text-error">*</span>
            </label>
            <select
              value={toShiftId}
              onChange={(e) => setToShiftId(e.target.value)}
              className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
            >
              {eligibleToShifts.length === 0 ? (
                <option value="">Không có ca OPEN phù hợp</option>
              ) : null}
              {eligibleToShifts.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name || "Ca"} · {s.date} · {fmtTime(s.startTime)}-{fmtTime(s.endTime)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
              Vị trí đích <span className="text-error">*</span>
            </label>
            <select
              value={toPositionId}
              onChange={(e) => setToPositionId(e.target.value)}
              disabled={!positionOptions.length}
              className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all disabled:opacity-60"
            >
              {!positionOptions.length ? <option value="">Không có vị trí cho ca</option> : null}
              {positionOptions.map((p) => (
                <option key={p.positionId} value={p.positionId}>
                  {p.positionName}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-2">
            Lý do (tùy chọn)
          </label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Nhập lý do đổi ca..."
            rows={3}
            className="w-full px-4 py-3 bg-surface-container-lowest rounded-xl border border-outline/20 text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all resize-none"
          />
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 text-on-surface-variant font-semibold rounded-lg hover:bg-surface-container-high transition-colors"
            disabled={saving}
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={saving || !toShiftId || !toPositionId}
            className="px-5 py-2.5 bg-primary text-on-primary font-semibold rounded-lg hover:bg-primary/90 transition-colors shadow-md disabled:opacity-50 flex items-center gap-2"
          >
            <span className="material-symbols-outlined">send</span>
            {saving ? "Đang gửi..." : "Gửi yêu cầu"}
          </button>
        </div>
      </form>
    </Modal>
  )
}

