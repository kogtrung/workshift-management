import { useEffect, useState } from 'react'
import { useParams, useOutletContext, Link } from 'react-router-dom'
import { getUnderstaffedAlerts } from '../features/alerts/alertApi'

function fmtTime(t) { return t ? String(t).substring(0, 5) : '—' }

export function AlertsPage() {
  const { groupId } = useParams()
  const { isManager } = useOutletContext() || {}

  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  async function loadAlerts() {
    setLoading(true); setError(null)
    try {
      const res = await getUnderstaffedAlerts(groupId)
      setAlerts(Array.isArray(res) ? res : (res?.data ?? []))
    } catch (err) {
      setError(err?.message || 'Không thể tải cảnh báo')
    } finally { setLoading(false) }
  }

  useEffect(() => { loadAlerts() }, [groupId])

  const totalShortage = alerts.reduce((s, a) => s + Math.max(0, (a.totalRequired || 0) - (a.totalApproved || 0)), 0)

  if (!isManager) {
    return (
      <div className="w-full text-center py-20">
        <span className="material-symbols-outlined text-5xl text-on-surface-variant opacity-20 mb-4">lock</span>
        <h3 className="text-xl font-bold text-on-surface mb-2">Không có quyền truy cập</h3>
        <p className="text-on-surface-variant">Chỉ Manager mới có thể xem cảnh báo.</p>
      </div>
    )
  }

  return (
    <div className="w-full space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">Quản lý</p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">Cảnh báo thiếu người</h2>
          <p className="text-on-surface-variant font-medium">Các ca đang mở nhưng chưa đủ nhân sự theo nhu cầu</p>
        </div>
        <button onClick={loadAlerts}
          className="px-5 py-2.5 bg-surface-container text-on-surface font-semibold rounded-lg hover:bg-surface-container-high transition-colors flex items-center gap-2 self-start">
          <span className="material-symbols-outlined text-sm">refresh</span>
          Làm mới
        </button>
      </div>

      {error && <div className="bg-error-container/20 text-on-error-container rounded-xl p-4 text-center">{error}</div>}
      {loading && <div className="text-center py-12"><p className="text-on-surface-variant animate-pulse">Đang tải...</p></div>}

      {/* Stats */}
      {!loading && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-error">{alerts.length}</p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Ca thiếu người</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-amber-600">{totalShortage}</p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Tổng thiếu hụt</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl border border-outline/10 p-4 text-center">
            <p className="text-3xl font-black text-on-surface">
              {alerts.reduce((s, a) => s + (a.totalRequired || 0), 0)}
            </p>
            <p className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1">Tổng nhu cầu</p>
          </div>
        </div>
      )}

      {/* Alerts list */}
      {!loading && alerts.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {alerts.map(alert => {
            const shortage = Math.max(0, (alert.totalRequired || 0) - (alert.totalApproved || 0))
            const pct = alert.totalRequired > 0 ? Math.round((alert.totalApproved / alert.totalRequired) * 100) : 0
            return (
              <div key={alert.shiftId}
                className="bg-surface-container-lowest rounded-2xl border border-outline/10 shadow-sm overflow-hidden hover:shadow-md transition-all">
                {/* Shift header */}
                <div className="px-5 py-4 bg-error-container/10 border-b border-error/10 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-error/10 flex items-center justify-center">
                      <span className="material-symbols-outlined text-error">warning</span>
                    </div>
                    <div>
                      <p className="text-sm font-bold text-on-surface">{alert.shiftName || 'Ca'}</p>
                      <p className="text-xs text-on-surface-variant">{alert.date} · {fmtTime(alert.startTime)} – {fmtTime(alert.endTime)}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-error/10 text-error text-xs font-bold">
                      <span className="material-symbols-outlined text-sm">person_off</span>
                      Thiếu {shortage}
                    </span>
                  </div>
                </div>

                {/* Progress overview */}
                <div className="px-5 py-3 border-b border-outline/5">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Nhân sự tổng</span>
                    <span className="text-xs font-bold text-on-surface">{alert.totalApproved}/{alert.totalRequired} ({pct}%)</span>
                  </div>
                  <div className="w-full h-2.5 bg-surface-container rounded-full overflow-hidden">
                    <div className={`h-full rounded-full transition-all ${pct >= 80 ? 'bg-amber-500' : pct >= 50 ? 'bg-orange-500' : 'bg-error'}`}
                      style={{ width: `${Math.min(100, pct)}%` }} />
                  </div>
                </div>

                {/* Position shortages */}
                <div className="px-5 py-3 space-y-2">
                  {(alert.shortages || []).map(ps => {
                    const posPct = ps.required > 0 ? Math.round((ps.approved / ps.required) * 100) : 0
                    return (
                      <div key={ps.positionId} className="flex items-center gap-3">
                        <div className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-[10px] font-bold text-on-surface-variant flex-shrink-0">
                          {(ps.positionName || '?').charAt(0).toUpperCase()}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-xs font-bold text-on-surface truncate">{ps.positionName}</span>
                            <span className="text-[10px] text-on-surface-variant font-medium">{ps.approved}/{ps.required}</span>
                          </div>
                          <div className="w-full h-1.5 bg-surface-container rounded-full overflow-hidden">
                            <div className={`h-full rounded-full ${posPct >= 100 ? 'bg-emerald-500' : posPct >= 50 ? 'bg-amber-500' : 'bg-error'}`}
                              style={{ width: `${Math.min(100, posPct)}%` }} />
                          </div>
                        </div>
                        {ps.shortage > 0 && (
                          <span className="text-[10px] font-bold text-error bg-error/10 px-2 py-0.5 rounded-full flex-shrink-0">
                            -{ps.shortage}
                          </span>
                        )}
                      </div>
                    )
                  })}
                </div>

                {/* Action */}
                <div className="px-5 py-3 border-t border-outline/5">
                  <Link to={`/groups/${groupId}/shifts`}
                    className="w-full py-2 text-xs font-bold text-primary bg-primary-container/20 hover:bg-primary-container/40 rounded-lg transition-colors flex items-center justify-center gap-1.5">
                    <span className="material-symbols-outlined text-sm">calendar_month</span>
                    Đi đến quản lý ca
                  </Link>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {!loading && alerts.length === 0 && (
        <div className="text-center py-16 bg-surface-container-lowest rounded-2xl border border-outline/10 border-dashed">
          <span className="material-symbols-outlined text-5xl text-emerald-500 opacity-40 mb-4">verified</span>
          <h3 className="text-xl font-bold text-on-surface mb-2">Tất cả ca đều đủ người!</h3>
          <p className="text-on-surface-variant font-medium">Không có ca nào đang thiếu nhân sự. Tuyệt vời! 🎉</p>
        </div>
      )}
    </div>
  )
}
