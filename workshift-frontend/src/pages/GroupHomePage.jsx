import { useState } from 'react'
import { Link, useParams, useOutletContext, useNavigate } from 'react-router-dom'
import { leaveGroup } from '../features/groups/groupApi'

export function GroupHomePage() {
  const { groupId } = useParams()
  const { groupInfo, isManager } = useOutletContext() || {}
  const navigate = useNavigate()
  const [leaving, setLeaving] = useState(false)

  async function handleLeave() {
    if (!confirm('Bạn có chắc chắn muốn rời group này?')) return
    setLeaving(true)
    try {
      await leaveGroup(groupId)
      navigate('/app/groups', { replace: true })
    } catch (err) {
      alert(err?.message || 'Không thể rời group')
    } finally {
      setLeaving(false)
    }
  }

  return (
    <div className="w-full space-y-8">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-bold tracking-[0.05em] uppercase text-on-surface-variant opacity-70">
            {isManager ? 'Manager Dashboard' : 'Staff Dashboard'}
          </p>
          <h2 className="text-3xl font-extrabold text-on-surface tracking-tight">
            {groupInfo?.groupName || `Group #${groupId}`}
          </h2>
          {groupInfo?.description && (
            <p className="text-on-surface-variant font-medium">{groupInfo.description}</p>
          )}
        </div>

        {/* Leave group button for Staff */}
        {!isManager && groupInfo && (
          <button
            onClick={handleLeave}
            disabled={leaving}
            className="px-5 py-2.5 bg-surface-container-lowest text-error font-semibold rounded-lg border border-error/20 hover:bg-error/5 transition-colors flex items-center gap-2 self-start"
          >
            <span className="material-symbols-outlined text-sm">logout</span>
            <span>{leaving ? 'Đang rời...' : 'Rời group'}</span>
          </button>
        )}
      </div>

      {/* Join Code Card */}
      {groupInfo?.joinCode && (
        <div className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 shadow-[0_24px_48px_rgba(0,52,94,0.06)] inline-flex items-center gap-6">
          <div className="w-12 h-12 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container">
            <span className="material-symbols-outlined">vpn_key</span>
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Mã tham gia Group</p>
            <p className="text-2xl font-black text-primary tracking-[0.15em]">{groupInfo.joinCode}</p>
            <p className="text-xs text-on-surface-variant mt-1">Chia sẻ mã này để mời thành viên mới</p>
          </div>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-6 bg-surface-container-lowest rounded-xl shadow-[0_24px_48px_rgba(0,52,94,0.06)] border border-outline/5">
          <div className="text-xs font-bold tracking-widest text-on-surface-variant uppercase mb-2">Thành viên</div>
          <div className="flex items-center gap-3">
            <div className="text-4xl font-black text-on-surface tracking-tighter">—</div>
            <Link to={`/groups/${groupId}/members`}
              className="text-xs font-bold text-primary hover:underline decoration-2 underline-offset-4">
              Xem danh sách →
            </Link>
          </div>
        </div>
        {isManager && (
          <div className="p-6 bg-surface-container-lowest rounded-xl shadow-[0_24px_48px_rgba(0,52,94,0.06)] border border-outline/5">
            <div className="text-xs font-bold tracking-widest text-on-surface-variant uppercase mb-2">Chờ duyệt</div>
            <div className="flex items-center gap-3">
              <div className="text-4xl font-black text-on-surface tracking-tighter">—</div>
              <Link to={`/groups/${groupId}/members/pending`}
                className="text-xs font-bold text-primary hover:underline decoration-2 underline-offset-4">
                Xem ngay →
              </Link>
            </div>
          </div>
        )}
        <div className="p-6 bg-surface-container-lowest rounded-xl shadow-[0_24px_48px_rgba(0,52,94,0.06)] border border-outline/5">
          <div className="text-xs font-bold tracking-widest text-on-surface-variant uppercase mb-2">Ca làm việc</div>
          <div className="flex items-center gap-3">
            <div className="text-4xl font-black text-on-surface tracking-tighter">—</div>
            <Link to={`/groups/${groupId}/shifts`}
              className="text-xs font-bold text-primary hover:underline decoration-2 underline-offset-4">
              Quản lý ca →
            </Link>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div>
        <h3 className="text-xs font-bold tracking-widest text-on-surface-variant uppercase mb-4">Thao tác nhanh</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <Link to={`/groups/${groupId}/members`}
            className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-lg font-extrabold text-on-surface tracking-tight">Danh sách thành viên</div>
                <div className="text-sm text-on-surface-variant font-medium mt-1">Xem tất cả thành viên trong group</div>
              </div>
              <div className="w-12 h-12 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container group-hover:bg-primary group-hover:text-on-primary transition-colors">
                <span className="material-symbols-outlined">group</span>
              </div>
            </div>
          </Link>

          {isManager && (
            <Link to={`/groups/${groupId}/members/pending`}
              className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-lg font-extrabold text-on-surface tracking-tight">Duyệt thành viên</div>
                  <div className="text-sm text-on-surface-variant font-medium mt-1">Xem yêu cầu tham gia group</div>
                </div>
                <div className="w-12 h-12 rounded-full bg-tertiary-container flex items-center justify-center text-on-tertiary-container group-hover:bg-tertiary group-hover:text-on-tertiary transition-colors">
                  <span className="material-symbols-outlined">person_add</span>
                </div>
              </div>
            </Link>
          )}

          {isManager && (
            <Link to={`/groups/${groupId}/positions`}
              className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-lg font-extrabold text-on-surface tracking-tight">Vị trí làm việc</div>
                  <div className="text-sm text-on-surface-variant font-medium mt-1">Quản lý vị trí: Pha chế, Thu ngân...</div>
                </div>
                <div className="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center text-amber-700 group-hover:bg-amber-500 group-hover:text-white transition-colors">
                  <span className="material-symbols-outlined">work</span>
                </div>
              </div>
            </Link>
          )}

          {isManager && (
            <Link to={`/groups/${groupId}/shift-templates`}
              className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-lg font-extrabold text-on-surface tracking-tight">Ca mẫu</div>
                  <div className="text-sm text-on-surface-variant font-medium mt-1">Cấu hình khung giờ ca mẫu</div>
                </div>
                <div className="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center text-purple-700 group-hover:bg-purple-500 group-hover:text-white transition-colors">
                  <span className="material-symbols-outlined">schedule</span>
                </div>
              </div>
            </Link>
          )}

          {isManager && (
            <Link to={`/groups/${groupId}/shifts`}
              className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-lg font-extrabold text-on-surface tracking-tight">Quản lý ca</div>
                  <div className="text-sm text-on-surface-variant font-medium mt-1">Tạo ca và cấu hình nhu cầu nhân sự</div>
                </div>
                <div className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center text-green-700 group-hover:bg-green-500 group-hover:text-white transition-colors">
                  <span className="material-symbols-outlined">calendar_month</span>
                </div>
              </div>
            </Link>
          )}

          <Link to={`/groups/${groupId}/audit-logs`}
            className="bg-surface-container-lowest rounded-2xl p-6 border border-outline/10 hover:bg-surface-container-low transition-all shadow-[0_24px_48px_rgba(0,52,94,0.06)] group">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-lg font-extrabold text-on-surface tracking-tight">Audit Timeline</div>
                <div className="text-sm text-on-surface-variant font-medium mt-1">Nhật ký hoạt động group</div>
              </div>
              <div className="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface group-hover:bg-primary group-hover:text-on-primary transition-colors">
                <span className="material-symbols-outlined">history</span>
              </div>
            </div>
          </Link>
        </div>
      </div>
    </div>
  )
}
