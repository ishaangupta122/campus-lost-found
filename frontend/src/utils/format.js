// Small display helpers shared by several pages.

export function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}

// "CLAIM_PENDING" -> "Claim pending"
export function formatLabel(value) {
  if (!value) return ''
  const text = value.replaceAll('_', ' ').toLowerCase()
  return text.charAt(0).toUpperCase() + text.slice(1)
}
