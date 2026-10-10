// Shared lists, so the report form and the filters always offer the same choices.
export const CATEGORIES = ['Bags', 'Books', 'Clothing', 'Electronics', 'ID cards', 'Keys', 'Stationery', 'Water bottles', 'Other']

export const MAX_IMAGE_MB = 5
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

// "Reported" time filter. days = how far back the report date may be (0 = today only).
export const PERIOD_OPTIONS = [
  { value: '', label: 'Any time' },
  { value: '0', label: 'Today' },
  { value: '7', label: 'Last 7 days' },
  { value: '30', label: 'Last 30 days' },
  { value: '90', label: 'Last 90 days' },
]
