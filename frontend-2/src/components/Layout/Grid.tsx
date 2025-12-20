import type {ReactNode} from 'react'
import clsx from 'clsx'

interface GridProps {
    children: ReactNode
    className?: string
    container?: boolean
    item?: boolean
    xs?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 'auto'
    sm?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 'auto'
    md?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 'auto'
    lg?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 'auto'
    xl?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 'auto'
    spacing?: 1 | 2 | 3 | 4 | 5 | 6 | 8 | 10
    direction?: 'row' | 'column'
    justifyContent?: 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly'
    alignItems?: 'flex-start' | 'center' | 'flex-end' | 'stretch' | 'baseline'
    component?: 'div' | 'section' | 'article' | 'main'
}

const colMap: { [key: number]: string } = {
    1: 'col-span-1',
    2: 'col-span-2',
    3: 'col-span-3',
    4: 'col-span-4',
    5: 'col-span-5',
    6: 'col-span-6',
    7: 'col-span-7',
    8: 'col-span-8',
    9: 'col-span-9',
    10: 'col-span-10',
    11: 'col-span-11',
    12: 'col-span-12',
}

const smColMap: { [key: number]: string } = {
    1: 'sm:col-span-1',
    2: 'sm:col-span-2',
    3: 'sm:col-span-3',
    4: 'sm:col-span-4',
    5: 'sm:col-span-5',
    6: 'sm:col-span-6',
    7: 'sm:col-span-7',
    8: 'sm:col-span-8',
    9: 'sm:col-span-9',
    10: 'sm:col-span-10',
    11: 'sm:col-span-11',
    12: 'sm:col-span-12',
}

const mdColMap: { [key: number]: string } = {
    1: 'md:col-span-1',
    2: 'md:col-span-2',
    3: 'md:col-span-3',
    4: 'md:col-span-4',
    5: 'md:col-span-5',
    6: 'md:col-span-6',
    7: 'md:col-span-7',
    8: 'md:col-span-8',
    9: 'md:col-span-9',
    10: 'md:col-span-10',
    11: 'md:col-span-11',
    12: 'md:col-span-12',
}

const lgColMap: { [key: number]: string } = {
    1: 'lg:col-span-1',
    2: 'lg:col-span-2',
    3: 'lg:col-span-3',
    4: 'lg:col-span-4',
    5: 'lg:col-span-5',
    6: 'lg:col-span-6',
    7: 'lg:col-span-7',
    8: 'lg:col-span-8',
    9: 'lg:col-span-9',
    10: 'lg:col-span-10',
    11: 'lg:col-span-11',
    12: 'lg:col-span-12',
}

const xlColMap: { [key: number]: string } = {
    1: 'xl:col-span-1',
    2: 'xl:col-span-2',
    3: 'xl:col-span-3',
    4: 'xl:col-span-4',
    5: 'xl:col-span-5',
    6: 'xl:col-span-6',
    7: 'xl:col-span-7',
    8: 'xl:col-span-8',
    9: 'xl:col-span-9',
    10: 'xl:col-span-10',
    11: 'xl:col-span-11',
    12: 'xl:col-span-12',
}

const spacingMap: { [key: number]: string } = {
    1: 'gap-1',
    2: 'gap-2',
    3: 'gap-3',
    4: 'gap-4',
    5: 'gap-5',
    6: 'gap-6',
    8: 'gap-8',
    10: 'gap-10',
}

const justifyMap = {
    'flex-start': 'justify-start',
    'center': 'justify-center',
    'flex-end': 'justify-end',
    'space-between': 'justify-between',
    'space-around': 'justify-around',
    'space-evenly': 'justify-evenly',
}

const alignMap = {
    'flex-start': 'items-start',
    'center': 'items-center',
    'flex-end': 'items-end',
    'stretch': 'items-stretch',
    'baseline': 'items-baseline',
}

/**
 * Grid component - Replaces MUI Grid
 * Responsive grid layout using CSS Grid
 */
export const Grid = ({
                         children,
                         className = '',
                         container = false,
                         item = false,
                         xs = 12,
                         sm,
                         md,
                         lg,
                         xl,
                         spacing = 2,
                         direction = 'row',
                         justifyContent,
                         alignItems,
                         component: Component = 'div',
                     }: GridProps) => {
    const isContainer = container || (!item && !xs && !sm && !md && !lg && !xl)

    if (isContainer) {
        const classes = clsx(
            'grid',
            direction === 'column' && 'grid-cols-1',
            direction === 'row' && 'grid-cols-12',
            spacing && spacingMap[spacing],
            justifyContent && justifyMap[justifyContent as keyof typeof justifyMap],
            alignItems && alignMap[alignItems as keyof typeof alignMap],
            className
        )

        return (
            <Component className={classes}>
                {children}
            </Component>
        )
    }

    // Item mode
    const classes = clsx(
        'w-full',
        xs && (typeof xs === 'number' ? colMap[xs] : 'col-span-auto'),
        sm && smColMap[sm as number],
        md && mdColMap[md as number],
        lg && lgColMap[lg as number],
        xl && xlColMap[xl as number],
        className
    )

    return (
        <Component className={classes}>
            {children}
        </Component>
    )
}

Grid.displayName = 'Grid'
