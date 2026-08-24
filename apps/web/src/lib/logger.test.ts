import { logger } from './logger'

describe('logger', () => {
  it('routes error level to console.error with timestamp and module tags', () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    try {
      logger.error('auth', 'login failed')
      expect(errorSpy).toHaveBeenCalledTimes(1)
      const [prefix, message] = errorSpy.mock.calls[0]
      expect(prefix).toMatch(/^\[\d{4}-\d{2}-\d{2}T.*\] \[ERROR\] \[auth\]$/)
      expect(message).toBe('login failed')
    } finally {
      errorSpy.mockRestore()
    }
  })

  it('routes warn level to console.warn', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    try {
      logger.warn('api', 'slow response')
      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('[WARN] [api]'), 'slow response')
    } finally {
      warnSpy.mockRestore()
    }
  })

  it('passes structured details through as an extra argument', () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    try {
      const details = { status: 500, path: '/api/v1/jobs' }
      logger.error('api', 'request failed', details)
      expect(errorSpy).toHaveBeenCalledWith(
        expect.stringContaining('[ERROR] [api]'),
        'request failed',
        details,
      )
    } finally {
      errorSpy.mockRestore()
    }
  })

  it('suppresses debug messages at the default info level', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {})
    try {
      logger.debug('parse', 'extracted fields', { count: 12 })
      expect(logSpy).not.toHaveBeenCalled()
    } finally {
      logSpy.mockRestore()
    }
  })
})
