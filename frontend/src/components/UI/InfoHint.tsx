import clsx from 'clsx'
import {Info} from 'lucide-react'
import {useEffect, useId, useLayoutEffect, useRef, useState} from 'react'
import {createPortal} from 'react-dom'

interface InfoHintProps {
    text: string
    className?: string
}

export const InfoHint = ({text, className}: InfoHintProps) => {
    const tooltipId = useId()
    const buttonRef = useRef<HTMLButtonElement>(null)
    const [hovered, setHovered] = useState(false)
    const [focused, setFocused] = useState(false)
    const [pinned, setPinned] = useState(false)
    const [coords, setCoords] = useState({top: 0, left: 0})
    const open = hovered || focused || pinned

    useLayoutEffect(() => {
        if (!open || !buttonRef.current) return
        const rect = buttonRef.current.getBoundingClientRect()
        setCoords({top: rect.top, left: rect.left + rect.width / 2})
    }, [open])

    // Touch devices have no hover/blur: tap outside or Escape is the only way to dismiss.
    useEffect(() => {
        if (!open) return
        const close = () => {
            setHovered(false)
            setFocused(false)
            setPinned(false)
        }
        const onPointerDown = (e: Event) => {
            if (!buttonRef.current?.contains(e.target as Node)) close()
        }
        const onKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') close()
        }
        document.addEventListener('pointerdown', onPointerDown)
        document.addEventListener('keydown', onKeyDown)
        return () => {
            document.removeEventListener('pointerdown', onPointerDown)
            document.removeEventListener('keydown', onKeyDown)
        }
    }, [open])

    return (
        <>
            <button
                ref={buttonRef}
                type="button"
                aria-label={text}
                aria-describedby={open ? tooltipId : undefined}
                className={clsx('inline-flex align-middle text-blue-600 hover:text-blue-700 rounded-full', className)}
                onMouseEnter={() => setHovered(true)}
                onMouseLeave={() => setHovered(false)}
                onFocus={() => setFocused(true)}
                onBlur={() => {
                    setFocused(false)
                    setPinned(false)
                }}
                onClick={(e) => {
                    e.stopPropagation()
                    setPinned((p) => !p)
                }}
            >
                <Info className="h-4 w-4" aria-hidden="true"/>
            </button>
            {open && createPortal(
                <span
                    id={tooltipId}
                    role="tooltip"
                    style={{position: 'fixed', top: coords.top, left: coords.left}}
                    className={clsx(
                        'pointer-events-none z-50 -translate-x-1/2 -translate-y-full -mt-1.5 max-w-[80vw]',
                        'rounded-md px-2.5 py-1.5 text-xs leading-relaxed',
                        'bg-surface-raised text-text-primary border border-border shadow-lg'
                    )}
                >
                    {text}
                </span>,
                document.body,
            )}
        </>
    )
}

InfoHint.displayName = 'InfoHint'
