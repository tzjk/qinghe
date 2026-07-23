export function formatDisplayName({ realName, nickname, phoneMasked } = {}) {
  return text(realName) || text(nickname) || text(phoneMasked) || '未设置'
}

export function text(value) {
  return String(value || '').trim()
}
