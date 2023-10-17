set noerrorbells visualbell t_vb=
autocmd GUIEnter * set visualbell t_vb=
set hlsearch
colo blue
set nowrap
set ic
set cursorline
set guifont=Menlo_Regular:h13 
syntax off
nnoremap <silent> <F3> :redir @a<CR>:g//<CR>:redir END<CR>:new<CR>:put! a<CR>

function! GitBranch()
  return system("git rev-parse --abbrev-ref HEAD 2>/dev/null | tr -d '\n'")
endfunction
set laststatus=2
set statusline=
set statusline+=%#PmenuSel#
set statusline+=%#LineNr#
set statusline+=\ %f
set statusline+=%m\
set statusline+=%=
set statusline+=%#CursorColumn#
set statusline+=\ %y
set statusline+=\ %p%%
set statusline+=\ %l:%c
set statusline+=\ 

command! -nargs=1 S let @/ = escape('<args>', '\')
nmap <Leader>S :execute(":S " . input('Regex-off: /'))<CR>

