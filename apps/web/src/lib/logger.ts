export type LogLevel = 'debug' | 'info' | 'warn' | 'error'

const LEVEL_ORDER: Record<LogLevel, number> = {
  debug: 10,
  info: 20,
  warn: 30,
  error: 40,
}

const MIN_LEVEL: LogLevel = (import.meta.env.VITE_LOG_LEVEL as LogLevel | undefined) ?? 'info'

function shouldLog(level: LogLevel): boolean {
  return LEVEL_ORDER[level] >= LEVEL_ORDER[MIN_LEVEL]
}

function emit(level: LogLevel, moduleName: string, message: string, details?: unknown): void {
  if (!shouldLog(level)) return
  const prefix = `[${new Date().toISOString()}] [${level.toUpperCase()}] [${moduleName}]`
  const method = level === 'debug' ? 'log' : level
  if (details !== undefined) {
    console[method](prefix, message, details)
  } else {
    console[method](prefix, message)
  }
}

export const logger = {
  debug(moduleName: string, message: string, details?: unknown): void {
    emit('debug', moduleName, message, details)
  },
  info(moduleName: string, message: string, details?: unknown): void {
    emit('info', moduleName, message, details)
  },
  warn(moduleName: string, message: string, details?: unknown): void {
    emit('warn', moduleName, message, details)
  },
  error(moduleName: string, message: string, details?: unknown): void {
    emit('error', moduleName, message, details)
  },
}
