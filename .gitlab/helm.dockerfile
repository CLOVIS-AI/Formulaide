FROM archlinux:latest

RUN pacman -Syuu --noconfirm helm kubectl
