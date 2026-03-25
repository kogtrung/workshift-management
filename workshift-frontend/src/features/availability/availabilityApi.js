import { apiFetch } from '../../api/apiClient'

export function getMyAvailability() {
  return apiFetch('/availability')
}

export function updateMyAvailability(items) {
  return apiFetch('/availability', {
    method: 'PUT',
    body: JSON.stringify({ items }),
  })
}
