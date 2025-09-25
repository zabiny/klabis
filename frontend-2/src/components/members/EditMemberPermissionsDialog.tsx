import {Dialog, DialogContent, DialogTitle, IconButton} from '@mui/material';
import {Close} from '@mui/icons-material';
import EditMemberPermissionsForm from './EditMemberPermissionsForm';

interface EditMemberPermissionsDialogProps {
    open: boolean;
    onClose: () => void;
    memberId: number;
}

const EditMemberPermissionsDialog = ({
                                         open,
                                         onClose,
                                         memberId
                                     }: EditMemberPermissionsDialogProps) => {
    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
            PaperProps={{
                sx: {minHeight: '400px'}
            }}
        >
            <DialogTitle sx={{m: 0, p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                Úprava oprávnění člena
                <IconButton
                    aria-label="close"
                    onClick={onClose}
                    sx={{
                        position: 'absolute',
                        right: 8,
                        top: 8,
                        color: (theme) => theme.palette.grey[500],
                    }}
                >
                    <Close/>
                </IconButton>
            </DialogTitle>
            <DialogContent dividers sx={{p: 0}}>
                <EditMemberPermissionsForm memberId={memberId}/>
            </DialogContent>
        </Dialog>
    );
};

export default EditMemberPermissionsDialog;
