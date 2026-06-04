export function getUrlParam(name) {
  const params = new URLSearchParams(window.location.search)
  return params.get(name)
}

export function setUrlParam(name, value) {
  const params = new URLSearchParams(window.location.search)
  if (value) {
    params.set(name, value)
  } else {
    params.delete(name)
  }
  const newUrl = `${window.location.pathname}?${params.toString()}`
  window.history.pushState({ path: newUrl }, '', newUrl)
}

export function getUrlParams() {
  const params = new URLSearchParams(window.location.search)
  const result = {}
  for (const [key, value] of params) {
    result[key] = value
  }
  return result
}

export function setUrlParams(params) {
  const urlParams = new URLSearchParams(window.location.search)
  for (const [key, value] of Object.entries(params)) {
    if (value) {
      urlParams.set(key, value)
    } else {
      urlParams.delete(key)
    }
  }
  const newUrl = `${window.location.pathname}?${urlParams.toString()}`
  window.history.pushState({ path: newUrl }, '', newUrl)
}

export function clearUrlParams() {
  window.history.pushState({ path: window.location.pathname }, '', window.location.pathname)
}