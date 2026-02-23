const createMemoryStorage = () => {
  let store = {}

  return {
    getItem(key) {
      return Object.prototype.hasOwnProperty.call(store, key) ? store[key] : null
    },
    setItem(key, value) {
      store[key] = String(value)
    },
    removeItem(key) {
      delete store[key]
    },
    clear() {
      store = {}
    }
  }
}

const hasStorageApi = (obj) => {
  return !!obj && typeof obj.getItem === 'function' && typeof obj.setItem === 'function' && typeof obj.clear === 'function'
}

const memoryStorage = createMemoryStorage()

if (!hasStorageApi(globalThis.localStorage)) {
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: memoryStorage
  })
}

if (typeof window !== 'undefined' && !hasStorageApi(window.localStorage)) {
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: globalThis.localStorage
  })
}
