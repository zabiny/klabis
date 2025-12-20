import {type ReactNode, useCallback} from "react";

const createIcon = (actionName: string): ReactNode | undefined => {
    const iconClass = "w-5 h-5 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300";

    switch (actionName) {
        case 'synchronize':
            return (
                <svg className={iconClass} fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd"
                          d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 1119.414 5.414 1 1 0 01-1.414-1.414A5.002 5.002 0 104.659 6.1V5a1 1 0 011-1h3a1 1 0 001-1V2a1 1 0 00-1-1H5a1 1 0 00-1 1zm9.707 2.293a1 1 0 00-1.414 1.414L14.586 9H11a1 1 0 100 2h5a1 1 0 001-1V5a1 1 0 00-1.707-.707l-1.586 1.586z"
                          clipRule="evenodd"/>
                </svg>
            );
        case 'createRegistration':
            return (
                <svg className={iconClass} fill="currentColor" viewBox="0 0 20 20">
                    <path
                        d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z"/>
                </svg>
            );
        case 'updateRegistration':
            return (
                <svg className={iconClass} fill="currentColor" viewBox="0 0 20 20">
                    <path
                        d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/>
                </svg>
            );
        case 'cancelRegistration':
            return (
                <svg className={iconClass} fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd"
                          d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                          clipRule="evenodd"/>
                </svg>
            );
        default:
            return undefined;
    }
}

type OnClickHandler = (s: string) => void;

export const Actions = ({
                            value, onClick
                        }: { value?: string[], onClick?: OnClickHandler }): React.ReactNode => {
    return (
        <div className="flex gap-2">{value && value.map(s => <Action key={s} actionName={s} onClick={onClick}/>)}</div>
    );
}

const Action = ({actionName, onClick}: { actionName: string, onClick?: OnClickHandler }): ReactNode => {
    const icon = createIcon(actionName);

    const event = useCallback(() => {
        if (onClick) onClick(actionName);
    }, [actionName, onClick]);

    if (icon) {
        return (
            <button
                onClick={event}
                title={actionName}
                className="inline-flex items-center justify-center cursor-pointer transition-colors group"
                aria-label={actionName}
            >
                {icon}
                <span
                    className="invisible group-hover:visible absolute bottom-full mb-2 px-2 py-1 bg-gray-900 dark:bg-gray-700 text-white text-xs rounded whitespace-nowrap">
                    {actionName}
                </span>
            </button>
        );
    } else {
        return (
            <button onClick={event} className="text-blue-600 dark:text-blue-400 hover:underline">
                {actionName}
            </button>
        );
    }
}
