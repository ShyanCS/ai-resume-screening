import './ErrorBanner.css'

interface ErrorBannerProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export function ErrorBanner({
  title = 'Something went wrong',
  message,
  onRetry,
}: ErrorBannerProps) {
  return (
    <div role="alert" className="error-banner">
      <strong className="error-banner-title">{title}</strong>
      {message ? <p className="error-banner-message">{message}</p> : null}
      {onRetry ? (
        <button type="button" className="error-banner-retry" onClick={onRetry}>
          Retry
        </button>
      ) : null}
    </div>
  )
}
