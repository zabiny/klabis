import clsx from 'clsx'
import {useId, useLayoutEffect, useRef, useState, type ReactNode} from 'react'
import {createPortal} from 'react-dom'

interface TooltipProps {
    content: ReactNode
    children: ReactNode
    className?: string
}

/**
 * Lightweight styled tooltip shown on hover/focus of its trigger.
 * Use instead of the native `title` attribute when the content needs
 * formatting (e.g. multi-line lists). Rendered through a portal so it is
 * never clipped by an ancestor's `overflow: hidden` (e.g. table wrappers).
 */
export const Tooltip = ({content, children, className}: TooltipProps) => {
    const tooltipId = useId()
    const triggerRef = useRef<HTMLSpanElement>(null)
    const [open, setOpen] = useState(false)
    const [coords, setCoords] = useState({top: 0, left: 0})

    useLayoutEffect(() => {
        if (!open || !triggerRef.current) return
        const rect = triggerRef.current.getBoundingClientRect()
        setCoords({top: rect.top, left: rect.left + rect.width / 2})
    }, [open])

    return (
        <span className={clsx('relative inline-flex', className)}>
            <span
                ref={triggerRef}
                tabIndex={0}
                aria-describedby={open ? tooltipId : undefined}
                className="inline-flex outline-none"
                onMouseEnter={() => setOpen(true)}
                onMouseLeave={() => setOpen(false)}
                onFocus={() => setOpen(true)}
                onBlur={() => setOpen(false)}
            >
                {children}
            </span>
            {open && createPortal(
                <span
                    id={tooltipId}
                    role="tooltip"
                    style={{position: 'fixed', top: coords.top, left: coords.left}}
                    className={clsx(
                        'pointer-events-none z-50 -translate-x-1/2 -translate-y-full -mt-1.5',
                        'whitespace-pre rounded-md px-2.5 py-1.5 text-xs leading-relaxed',
                        'bg-surface-raised text-text-primary border border-border shadow-lg'
                    )}
                >
                    {content}
                </span>,
                document.body,
            )}
        </span>
    )
}

Tooltip.displayName = 'Tooltip'
