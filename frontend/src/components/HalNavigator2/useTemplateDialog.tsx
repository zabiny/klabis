import {type ReactElement, type ReactNode, useCallback, useState} from 'react';
import {createPortal} from 'react-dom';
import type {HalFormsTemplate} from '../../api';
import {type HalTemplateAction} from '../../api/halResource';
import {getDialogTitleLabel, getTemplateLabel} from '../../localization';
import {Modal} from '../UI';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import type {HalFormFieldFactory} from './halforms';

export const defaultDialogTitle = (name: string, template: HalFormsTemplate): string | undefined =>
    getDialogTitleLabel(name) ?? getTemplateLabel(name) ?? template.title;

export interface TemplateDialogOptions {
    title?: string | ((template: HalFormsTemplate, name: string) => string);
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
    note?: ReactNode;
    submitLabel?: string;
    successMessage?: string;
    onSubmitSuccess?: (responseData?: unknown) => void;
    onSubmitError?: (error: unknown) => boolean | void;
    navigateOnSuccess?: boolean;
    fieldsFactory?: HalFormFieldFactory;
    initialData?: Record<string, unknown>;
    postprocessPayload?: (payload: Record<string, unknown>, template: HalFormsTemplate) => Record<string, unknown>;
}

export type TemplateDialogOverrides = Pick<
    TemplateDialogOptions,
    'title' | 'note' | 'onSubmitSuccess' | 'successMessage' | 'navigateOnSuccess' | 'initialData'
>;

export interface TemplateDialog {
    open: (action: HalTemplateAction | null | undefined, overrides?: TemplateDialogOverrides) => void;
    dialog: ReactElement | null;
}

interface OpenDialog {
    action: HalTemplateAction;
    overrides?: TemplateDialogOverrides;
}

/**
 * Modal engine for HAL-FORMS actions. The caller resolves the action through `halResource(...)`,
 * so the hook needs no resource and no template-name lookup of its own: an `undefined` action
 * simply does not open, which is how the server expresses that the action is unavailable.
 */
export function useTemplateDialog(options: TemplateDialogOptions = {}): TemplateDialog {
    const [opened, setOpened] = useState<OpenDialog | null>(null);

    const open = useCallback((action: HalTemplateAction | null | undefined, overrides?: TemplateDialogOverrides) => {
        if (action) {
            setOpened({action, overrides});
        }
    }, []);

    const close = useCallback(() => setOpened(null), []);

    let dialog: ReactElement | null = null;
    if (opened) {
        const {action} = opened;
        const effective = {...options, ...opened.overrides};
        const title = typeof effective.title === 'function'
            ? effective.title(action.template, action.name)
            : effective.title ?? action.dialogTitle;

        dialog = createPortal(
            <Modal isOpen onClose={close} title={title} size={effective.size ?? 'md'} context={effective.note}>
                <HalFormDisplay
                    template={action.template}
                    templateName={action.name}
                    href={action.href}
                    resourceData={(effective.initialData ?? {}) as Record<string, unknown>}
                    onClose={close}
                    onSubmitSuccess={(data) => {
                        effective.onSubmitSuccess?.(data);
                        close();
                    }}
                    onSubmitError={effective.onSubmitError}
                    successMessage={effective.successMessage ?? action.successMessage}
                    submitButtonLabel={effective.submitLabel}
                    navigateOnSuccess={effective.navigateOnSuccess}
                    fieldsFactory={effective.fieldsFactory}
                    postprocessPayload={effective.postprocessPayload}
                />
            </Modal>,
            document.body,
        );
    }

    return {open, dialog};
}
