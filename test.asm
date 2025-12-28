global _start

_start:
	mov rax, 0x2000001
	mov rdi, 4
	neg rdi
	syscall