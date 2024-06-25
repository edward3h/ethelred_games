<script lang="ts">
	import { createEventDispatcher, tick } from "svelte";

    let show = false;
    export let name:string;
    const dispatch = createEventDispatcher();

    let dialog: HTMLDialogElement;
    let textRef: HTMLInputElement;

    function updateName() {
        fetch('/api/player/name', {
            method: 'POST',
			headers: {
				'Content-Type': 'text/plain'
			},
            body: name
        })
        .then(close, close);
    }

    async function open() {
        show = true;
        dialog.showModal();
        dispatch('open');
        await tick();
        textRef.focus();
        textRef.select();
    }

    function close() {
        show = false;
        dialog.close();
        dispatch('close');
    }

    function handleEscape(event:KeyboardEvent) {
        if (event.key == 'Escape') {
            close();
        }
    }
</script>
<svelte:window on:keydown={handleEscape} on:click={close}/>
<div class="rel">
<dialog bind:this={dialog} class="modal">
    <!-- svelte-ignore a11y-autofocus -->
    <div class="modal-box">
    <input class="input input-bordered" maxlength="20" type="text" bind:this={textRef} bind:value={name} on:change={updateName} on:click|stopPropagation>
    <button class="btn" on:click|stopPropagation={updateName}>Update Name</button>
    </div>
</dialog>
<button class="btn" on:click|stopPropagation={open} disabled={show}>Change Name</button>
</div>

<style>
    .rel {
        position: relative;
    }
</style>