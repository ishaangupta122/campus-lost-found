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

// Keeps only items whose report date is within the last `days` days (today included).
// period is the value from the time filter: '' = no limit, '0' = today, '7' = last 7 days, ...
export function filterByPeriod(items, period) {
  if (period === '' || period == null) return items
  const cutoff = new Date()
  cutoff.setHours(0, 0, 0, 0)
  cutoff.setDate(cutoff.getDate() - Number(period))
  return items.filter((item) => new Date(`${item.reportDate}T00:00:00`) >= cutoff)
}
